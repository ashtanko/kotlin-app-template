// Top-level build file. Owns whole-repo concerns: static-analysis wiring that spans every
// module, coverage aggregation across `app`/`core`, README generation inputs, and git hooks.
// Per-module toolchain/lint/test config lives in the `template.kotlin-library` convention
// plugin (buildSrc/src/main/kotlin/template.kotlin-library.gradle.kts), applied by each module.
plugins {
    base
    idea
    jacoco
    id("com.github.nbaztec.coveralls-jacoco") version "1.2.20"
    // No version: com.diffplug.spotless is already on the classpath via buildSrc's own
    // dependency on it (needed there to reference SpotlessPlugin below and in the convention
    // plugin), so an `alias(libs.plugins.spotless)` here would conflict on the unversioned copy.
    id("com.diffplug.spotless")
    // Diktat's Gradle plugin registers a root-level `mergeDiktatReports` aggregation task the
    // moment any subproject applies it, so the root project needs the plugin present too (no
    // `diktat { }` config here — there's no Kotlin source at the root to lint).
    id("com.saveourtool.diktat")
    alias(libs.plugins.dependency.analysis)
}

fun isLinux(): Boolean {
    val osName = System.getProperty("os.name").lowercase()
    return listOf("linux", "mac os", "macos").contains(osName)
}

jacoco {
    toolVersion = "0.8.15"
}

spotless {
    kotlin {
        target(
            fileTree(
                mapOf(
                    "dir" to ".",
                    "include" to listOf("**/*.kt"),
                    "exclude" to listOf("**/build/**", "**/spotless/*.kt"),
                ),
            ),
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
        val delimiter = "^(package|object|import|interface|internal|@file|//startfile)"
        val licenseHeaderFile = rootProject.file("spotless/copyright.kt")
        licenseHeaderFile(licenseHeaderFile, delimiter)
    }
}

subprojects {
    apply<com.diffplug.gradle.spotless.SpotlessPlugin>()
}

// region Coverage aggregation across app + core
// Kover's built-in cross-project aggregation (`dependencies { kover(project(...)) }` at the
// root) currently fails to resolve kotlin-stdlib:2.4.10 (published as a Kotlin Multiplatform
// module) inside its internal `koverExternalArtifacts` configuration — a Kover 0.9.9 limitation,
// reproducible even outside this refactor, and there's no newer Kover release yet. So Kover is
// instead applied per module (by the convention plugin, alongside detekt/ktlint/diktat) and each
// module enforces its own >=80% bound; `./gradlew koverVerify`/`koverXmlReport` from the root
// still fans out to `:app`/`:core` the same way `./gradlew detekt` does.

// Jacoco has no built-in aggregation either, so the merge is hand-rolled: gather each module's
// exec data + class/source dirs into one root-level report and verification task. The output
// path intentionally matches the jacoco plugin's own per-module default
// (build/reports/jacoco/test/jacocoTestReport.xml) so CI, Codecov and coveralls-jacoco don't
// need to know this is now an aggregate.
val coverageModules = listOf(project(":app"), project(":core"))

// (The jacoco plugin only auto-registers jacocoTestReport/jacocoTestCoverageVerification when
// the `java` plugin is present, which the root project doesn't have — so these are freshly
// registered rather than configuring existing ones.)
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates an aggregated Jacoco coverage report for app + core."
    dependsOn(coverageModules.map { it.tasks.named("test") })

    val mainClassDirs = coverageModules.map { it.layout.buildDirectory.dir("classes/kotlin/main") }
    val mainSourceDirs = coverageModules.map { it.layout.projectDirectory.dir("src/main/kotlin") }
    val execFiles = coverageModules.map { it.layout.buildDirectory.file("jacoco/test.exec") }

    classDirectories.setFrom(mainClassDirs)
    sourceDirectories.setFrom(mainSourceDirs)
    additionalSourceDirs.setFrom(mainSourceDirs)
    executionData.setFrom(execFiles.filter { it.get().asFile.exists() })

    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
        csv.required.set(true)
        csv.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.csv"))
    }
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Enforces the aggregated Jacoco coverage floor across app + core."
    dependsOn("jacocoTestReport")

    val mainClassDirs = coverageModules.map { it.layout.buildDirectory.dir("classes/kotlin/main") }
    val mainSourceDirs = coverageModules.map { it.layout.projectDirectory.dir("src/main/kotlin") }
    val execFiles = coverageModules.map { it.layout.buildDirectory.file("jacoco/test.exec") }

    classDirectories.setFrom(mainClassDirs)
    sourceDirectories.setFrom(mainSourceDirs)
    additionalSourceDirs.setFrom(mainSourceDirs)
    executionData.setFrom(execFiles.filter { it.get().asFile.exists() })

    violationRules {
        rule {
            limit {
                minimum = "0.5".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
// endregion

// region Detekt markdown merge (feeds `make md` / config/main.md README generation)
// Detekt itself is applied per module by the convention plugin; running `./gradlew detekt` from
// the root fans out to `:app:detekt` and `:core:detekt` automatically. This task stitches their
// markdown reports together at the path `make md` expects (build/reports/detekt/detekt.md).
tasks.register("detektMergeMd") {
    group = "reporting"
    description = "Merges per-module Detekt markdown reports for README generation."
    dependsOn(":app:detekt", ":core:detekt")
    val moduleReports = listOf(project(":app"), project(":core")).map {
        it.name to it.layout.buildDirectory.file("reports/detekt/detekt.md")
    }
    val target = layout.buildDirectory.file("reports/detekt/detekt.md")
    doLast {
        target.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                moduleReports.joinToString("\n\n") { (name, report) ->
                    val file = report.get().asFile
                    "## Module: $name\n\n" + if (file.exists()) file.readText() else "_no report generated_"
                },
            )
        }
    }
}
// endregion

// region Git hooks
tasks {
    register<Copy>("copyGitHooks") {
        description = "Copies the git hooks from scripts/git-hooks to the .git folder."
        group = "git hooks"
        from("$rootDir/scripts/git-hooks/") {
            include("**/*.sh")
            rename("(.*).sh", "$1")
        }
        into("$rootDir/.git/hooks")
    }

    register<Exec>("installGitHooks") {
        description = "Installs the pre-commit git hooks from scripts/git-hooks."
        group = "git hooks"
        workingDir(rootDir)
        commandLine("chmod")
        args("-R", "+x", ".git/hooks/")
        dependsOn(named("copyGitHooks"))
        onlyIf {
            isLinux()
        }
        doLast {
            logger.info("Git hooks installed successfully.")
        }
    }

    register<Delete>("deleteGitHooks") {
        description = "Delete the pre-commit git hooks."
        group = "git hooks"
        delete(fileTree(".git/hooks/"))
    }

    afterEvaluate {
        tasks["clean"].dependsOn(tasks.named("installGitHooks"))
    }
}
// endregion
