#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly PROJECT_ROOT

new_project_name=""
new_package=""
old_project_name=""
old_package=""
github_owner=""
main_class=""
dry_run=false
keep_main_class=false

usage() {
    cat <<'USAGE'
Usage:
  scripts/rename-project.sh -n <name> -p <package> [options]
  scripts/rename-project.sh --project-name <name> --package <package> [options]
  scripts/rename-project.sh <name> <package> [options]

Renames the Gradle project and Kotlin package throughout the repository.

Required:
  -n, --project-name <name>   New Gradle/repository project name, for example my-app
  -p, --package <package>     New Kotlin package, for example com.example.myapp

Options:
  --old-project-name <name>   Override the project name detected from settings.gradle.kts
  --old-package <package>     Override the package detected from src/**/*.kt
  --github-owner <owner>      Replace the existing GitHub owner in badge/repository URLs
  --main-class <class>        Set application.mainClass. A simple class name is prefixed with the new package
  --keep-main-class           Leave application.mainClass unchanged
  --dry-run                   Print the planned changes without writing files
  -h, --help                  Show this help

Examples:
  scripts/rename-project.sh my-service com.acme.service
  scripts/rename-project.sh -n my-service -p com.acme.service
  scripts/rename-project.sh --project-name my-service --package com.acme.service --github-owner acme
USAGE
}

die() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

log() {
    printf '%s\n' "$*"
}

while (($# > 0)); do
    case "$1" in
        -n|--project-name)
            option="$1"
            shift
            (($# > 0)) || die "$option requires a value"
            new_project_name="$1"
            ;;
        -p|--package|--new-package)
            option="$1"
            shift
            (($# > 0)) || die "$option requires a value"
            new_package="$1"
            ;;
        --old-project-name)
            option="$1"
            shift
            (($# > 0)) || die "$option requires a value"
            old_project_name="$1"
            ;;
        --old-package)
            option="$1"
            shift
            (($# > 0)) || die "$option requires a value"
            old_package="$1"
            ;;
        --github-owner)
            option="$1"
            shift
            (($# > 0)) || die "$option requires a value"
            github_owner="$1"
            ;;
        --main-class)
            option="$1"
            shift
            (($# > 0)) || die "$option requires a value"
            main_class="$1"
            ;;
        --keep-main-class)
            keep_main_class=true
            ;;
        --dry-run)
            dry_run=true
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            die "unknown option: $1"
            ;;
        *)
            if [[ -z "$new_project_name" ]]; then
                new_project_name="$1"
            elif [[ -z "$new_package" ]]; then
                new_package="$1"
            else
                die "unexpected argument: $1"
            fi
            ;;
    esac
    shift
done

[[ -n "$new_project_name" ]] || die "new project name is required"
[[ -n "$new_package" ]] || die "new package is required"
[[ "$new_project_name" != *"/"* ]] || die "project name must not contain '/'"
[[ "$new_project_name" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] ||
    die "project name may contain only letters, numbers, '.', '_' and '-'"
[[ "$new_package" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$ ]] ||
    die "package must be lowercase dot-separated identifiers, for example com.example.app"
[[ -z "$github_owner" || "$github_owner" =~ ^[A-Za-z0-9_.-]+$ ]] ||
    die "GitHub owner may contain only letters, numbers, '.', '_' and '-'"

detect_project_name() {
    local settings_file="$PROJECT_ROOT/settings.gradle.kts"

    [[ -f "$settings_file" ]] || return 0
    perl -ne "if (/rootProject\\.name\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]/) { print \$1; exit }" "$settings_file"
}

common_package_prefix() {
    local prefix="$1"
    local package_name

    shift
    for package_name in "$@"; do
        while [[ "$package_name" != "$prefix" && "$package_name" != "$prefix".* ]]; do
            [[ "$prefix" == *.* ]] || return 1
            prefix="${prefix%.*}"
        done
    done

    printf '%s\n' "$prefix"
}

detect_package_name() {
    local package_names=()
    local source_file
    local package_name
    local unique_packages=()

    [[ -d "$PROJECT_ROOT/src" ]] || return 0
    while IFS= read -r -d '' source_file; do
        package_name="$(
            perl -ne \
                'if (/^\s*package\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*$/) { print $1; exit }' \
                "$source_file"
        )"
        [[ -n "$package_name" ]] && package_names+=("$package_name")
    done < <(find "$PROJECT_ROOT/src" -type f -name '*.kt' -print0)

    ((${#package_names[@]} > 0)) || return 0
    while IFS= read -r package_name; do
        unique_packages+=("$package_name")
    done < <(printf '%s\n' "${package_names[@]}" | sort -u)

    if ((${#unique_packages[@]} == 1)); then
        printf '%s\n' "${unique_packages[0]}"
    else
        common_package_prefix "${unique_packages[@]}"
    fi
}

old_project_name="${old_project_name:-$(detect_project_name)}"
old_package="${old_package:-$(detect_package_name)}"

[[ -n "$old_project_name" ]] || die "could not detect old project name; pass --old-project-name"
[[ -n "$old_package" ]] || die "could not detect old package; pass --old-package"
[[ "$old_package" =~ ^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+$ ]] ||
    die "old package is not a valid dot-separated package: $old_package"

readonly old_package_path="${old_package//./\/}"
readonly new_package_path="${new_package//./\/}"
readonly old_project_key="${old_project_name//[.-]/_}"
readonly new_project_key="${new_project_name//[.-]/_}"

declare -a replacement_from=()
declare -a replacement_to=()

add_replacement() {
    local from="$1"
    local to="$2"

    [[ -n "$from" && "$from" != "$to" ]] || return 0
    replacement_from+=("$from")
    replacement_to+=("$to")
}

detect_github_owner() {
    local file
    local owner

    for file in "$PROJECT_ROOT/README.md" "$PROJECT_ROOT/config/main.md"; do
        [[ -f "$file" ]] || continue
        owner="$(perl -ne "if (m{github\\.com/([^/[:space:]<>]+)/\\Q$old_project_name\\E}) { print \$1; exit }" "$file")"
        if [[ -n "$owner" ]]; then
            printf '%s\n' "$owner"
            return 0
        fi
    done
}

current_github_owner="$(detect_github_owner)"

add_replacement "$old_package" "$new_package"
add_replacement "$old_package_path" "$new_package_path"
add_replacement "$old_project_name" "$new_project_name"
add_replacement "$old_project_key" "$new_project_key"

if [[ -n "$github_owner" && -n "$current_github_owner" && "$github_owner" != "$current_github_owner" ]]; then
    add_replacement "github.com/$current_github_owner/" "github.com/$github_owner/"
    add_replacement "codecov.io/gh/$current_github_owner/" "codecov.io/gh/$github_owner/"
    add_replacement "repository/github/$current_github_owner/" "repository/github/$github_owner/"
    add_replacement "app.codacy.com/gh/$current_github_owner/" "app.codacy.com/gh/$github_owner/"
    add_replacement "${current_github_owner}_" "${github_owner}_"
fi

is_candidate_file() {
    case "$1" in
        *.kt|*.kts|*.java|*.groovy|*.gradle|*.xml|*.yml|*.yaml|*.properties|*.md|*.txt|*.sh|*.bash|*.zsh|*.bat|*.json|*.toml)
            return 0
            ;;
        *.gitignore|*.gitattributes|*.editorconfig|Makefile|LICENSE|README)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

list_repo_files() {
    if git -C "$PROJECT_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        git -C "$PROJECT_ROOT" ls-files -z --cached --others --exclude-standard
    else
        (
            cd "$PROJECT_ROOT"
            find . \
                \( -path './.git' -o -path './.gradle' -o -path './build' -o -path './.idea' -o -path './.kotlin' \) -prune \
                -o -type f -print0 |
                perl -0pe 's#^\./##'
        )
    fi
}

file_has_replacement() {
    local file="$1"
    local index

    for index in "${!replacement_from[@]}"; do
        if grep -F -q -- "${replacement_from[$index]}" "$file"; then
            return 0
        fi
    done

    return 1
}

apply_text_replacements() {
    local changed_files=()
    local relative_path
    local file
    local index

    while IFS= read -r -d '' relative_path; do
        is_candidate_file "$relative_path" || continue
        file="$PROJECT_ROOT/$relative_path"
        [[ -f "$file" ]] || continue
        file_has_replacement "$file" || continue

        changed_files+=("$relative_path")
        if [[ "$dry_run" == false ]]; then
            for index in "${!replacement_from[@]}"; do
                FROM="${replacement_from[$index]}" TO="${replacement_to[$index]}" \
                    perl -0pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$file"
            done
        fi
    done < <(list_repo_files)

    if ((${#changed_files[@]} == 0)); then
        log "Text replacements: no matching files"
        return 0
    fi

    log "Text replacements: ${#changed_files[@]} file(s)"
    printf '  %s\n' "${changed_files[@]}"
}

replace_literal_in_candidate_files() {
    local from="$1"
    local to="$2"
    local relative_path
    local file

    [[ -n "$from" && "$from" != "$to" ]] || return 0

    while IFS= read -r -d '' relative_path; do
        is_candidate_file "$relative_path" || continue
        file="$PROJECT_ROOT/$relative_path"
        [[ -f "$file" ]] || continue
        grep -F -q -- "$from" "$file" || continue

        FROM="$from" TO="$to" perl -0pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$file"
    done < <(list_repo_files)
}

configured_main_class() {
    local build_file="$PROJECT_ROOT/build.gradle.kts"

    [[ -f "$build_file" ]] || return 0
    perl -ne "if (/mainClass\\.set\\(\\s*['\\\"]([^'\\\"]+)['\\\"]\\s*\\)/) { print \$1; exit }" "$build_file"
}

normalize_main_class() {
    local value="$1"

    if [[ "$value" == *.* ]]; then
        printf '%s\n' "$value"
    else
        printf '%s.%s\n' "$new_package" "$value"
    fi
}

apply_gradle_and_tool_config() {
    local build_file="$PROJECT_ROOT/build.gradle.kts"
    local settings_file="$PROJECT_ROOT/settings.gradle.kts"
    local diktat_file="$PROJECT_ROOT/diktat-analysis.yml"
    local resolved_main_class="$main_class"
    local existing_main_class

    existing_main_class="$(configured_main_class)"
    if [[ "$keep_main_class" == false && -z "$resolved_main_class" ]]; then
        if [[ -n "$existing_main_class" ]]; then
            resolved_main_class="$new_package.${existing_main_class##*.}"
        fi
    fi

    if [[ "$keep_main_class" == false && -n "$resolved_main_class" ]]; then
        resolved_main_class="$(normalize_main_class "$resolved_main_class")"
        [[ "$resolved_main_class" =~ ^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+$ ]] ||
            die "main class must be a valid fully-qualified class name: $resolved_main_class"
    fi

    if [[ "$dry_run" == true ]]; then
        log "Config updates:"
        log "  settings.gradle.kts rootProject.name -> $new_project_name"
        log "  diktat-analysis.yml domainName -> $new_package"
        log "  Pitest targetClasses/targetTests -> $new_package.*"
        if [[ "$keep_main_class" == true ]]; then
            log "  application.mainClass -> unchanged"
        elif [[ -n "$resolved_main_class" ]]; then
            log "  application.mainClass -> $resolved_main_class"
        else
            log "  application.mainClass -> unchanged (not configured)"
        fi
        return 0
    fi

    if [[ "$keep_main_class" == false && -n "$existing_main_class" && -n "$resolved_main_class" ]]; then
        replace_literal_in_candidate_files "$existing_main_class" "$resolved_main_class"
    fi

    if [[ -f "$settings_file" ]]; then
        PROJECT_NAME="$new_project_name" \
            perl -0pi -e 's/(rootProject\.name\s*=\s*["\x27])([^"\x27]+)(["\x27])/$1$ENV{PROJECT_NAME}$3/g' \
            "$settings_file"
    fi

    if [[ -f "$diktat_file" ]]; then
        DOMAIN_NAME="$new_package" \
            perl -0pi -e 's/(domainName:\s*)\S+/$1$ENV{DOMAIN_NAME}/g' "$diktat_file"
    fi

    if [[ -f "$build_file" ]]; then
        PITEST_TARGET="$new_package.*" \
            perl -0pi -e 's/(targetClasses\.set\(setOf\(["\x27])[^"\x27]+(["\x27]\)\))/$1$ENV{PITEST_TARGET}$2/g; s/(targetTests\.set\(setOf\(["\x27])[^"\x27]+(["\x27]\)\))/$1$ENV{PITEST_TARGET}$2/g' \
            "$build_file"

        if [[ "$keep_main_class" == false && -n "$resolved_main_class" ]]; then
            MAIN_CLASS="$resolved_main_class" \
                perl -0pi -e 's/(mainClass\.set\(\s*["\x27])[^"\x27]+(["\x27]\s*\))/$1$ENV{MAIN_CLASS}$2/g' \
                "$build_file"
        fi
    fi
}

prune_empty_dirs() {
    local current_dir="$1"
    local stop_dir="$2"

    while [[ "$current_dir" != "$stop_dir" && "$current_dir" != "$PROJECT_ROOT" ]]; do
        rmdir "$current_dir" 2>/dev/null || break
        current_dir="$(dirname "$current_dir")"
    done
}

move_package_directories() {
    local source_root
    local old_dir
    local new_dir
    local temp_dir
    local moved_count=0

    [[ -d "$PROJECT_ROOT/src" ]] || return 0
    while IFS= read -r -d '' source_root; do
        old_dir="$source_root/$old_package_path"
        new_dir="$source_root/$new_package_path"

        [[ "$old_dir" != "$new_dir" ]] || continue
        [[ -d "$old_dir" ]] || continue

        ((moved_count += 1))
        log "Package directory: ${old_dir#"$PROJECT_ROOT"/} -> ${new_dir#"$PROJECT_ROOT"/}"

        if [[ "$dry_run" == true ]]; then
            continue
        fi

        [[ ! -e "$new_dir" ]] || die "target package directory already exists: ${new_dir#"$PROJECT_ROOT"/}"
        temp_dir="$(mktemp -d "$PROJECT_ROOT/.rename-project.XXXXXX")"
        mv "$old_dir" "$temp_dir/package"
        mkdir -p "$(dirname "$new_dir")"
        mv "$temp_dir/package" "$new_dir"
        rmdir "$temp_dir"
        prune_empty_dirs "$(dirname "$old_dir")" "$source_root"
    done < <(find "$PROJECT_ROOT/src" -type d -name kotlin -print0)

    if ((moved_count == 0)); then
        log "Package directories: no matching directories"
    fi
}

log "Project root: $PROJECT_ROOT"
log "Project name: $old_project_name -> $new_project_name"
log "Package: $old_package -> $new_package"
if [[ -n "$github_owner" ]]; then
    if [[ -n "$current_github_owner" ]]; then
        log "GitHub owner: $current_github_owner -> $github_owner"
    else
        log "GitHub owner: no existing owner detected"
    fi
fi

apply_text_replacements
apply_gradle_and_tool_config
move_package_directories

if [[ "$dry_run" == true ]]; then
    log "Dry run complete. No files were changed."
else
    log "Project rename complete."
fi
