/*
 * Copyright 2022 Oleksii Shtanko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.util.Locale

val projectJvmTarget = "11"
val satisfyingNumberOfCores = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
val ktlint: Configuration by configurations.creating
val isK2Enabled = true
val k2CompilerArg = if (isK2Enabled) listOf("-Xuse-k2") else emptyList()

fun isLinux(): Boolean {
    val osName = System.getProperty("os.name").toLowerCase(Locale.ROOT)
    return listOf("linux", "mac os", "macos").contains(osName)
}

@Suppress("DSL_SCOPE_VIOLATION") // https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    application
    jacoco
    idea
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("org.jetbrains.dokka") version "1.6.21"
    id("com.diffplug.spotless") version "6.3.0"
    id("com.autonomousapps.dependency-analysis") version "1.0.0-rc01"
    id("info.solidsoft.pitest") version "1.7.4"
    alias(libs.plugins.kt.jvm)
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.kotlin.link") }
    gradlePluginPortal()
    maven("https://plugins.gradle.org/m2/")
}

application {
    mainClass.set("link.kotlin.scripts.Application")
}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlintCheck by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "src/**/*.kt")
}

plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
        jvmArgs.set(listOf("-Xmx1024m"))
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("dev.shtanko.algorithms.*"))
        targetTests.set(setOf("dev.shtanko.algorithms.*"))
        pitestVersion.set("1.4.11")
        verbose.set(true)
        threads.set(System.getenv("PITEST_THREADS")?.toInt() ?: satisfyingNumberOfCores)
        outputFormats.set(setOf("XML", "HTML"))
        testPlugin.set("junit5")
        junit5PluginVersion.set("0.12")
    }
}

spotless {
    kotlin {
        target(
            fileTree(
                mapOf(
                    "dir" to ".",
                    "include" to listOf("**/*.kt"),
                    "exclude" to listOf("**/build/**", "**/spotless/*.kt")
                )
            )
        )
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
        val delimiter = "^(package|object|import|interface|internal|@file|//startfile)"
        val licenseHeaderFile = rootProject.file("spotless/copyright.kt")
        licenseHeaderFile(licenseHeaderFile, delimiter)
    }
}

subprojects {
    apply<com.diffplug.gradle.spotless.SpotlessPlugin>()
}

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

    jacocoTestReport {
        reports {
            html.required.set(true)
            xml.required.set(true)
            xml.outputLocation.set(file("$buildDir/reports/jacoco/report.xml"))
        }
        executionData(file("build/jacoco/test.exec"))
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = projectJvmTarget
            freeCompilerArgs = freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental" + k2CompilerArg
        }
    }

    withType<io.gitlab.arturbosch.detekt.Detekt> {
        description = "Runs over whole code base without the starting overhead for each module."
        parallel = true
        baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
        config.from(file("config/detekt/detekt.yml"))
        jvmTarget = projectJvmTarget

        setSource(files("src/main/kotlin", "src/test/kotlin"))
        include("**/*.kt")
        include("**/*.kts")
        exclude(".*/resources/.*")
        exclude(".*/build/.*")
        exclude("/versions.gradle.kts")

        reports {
            xml.required.set(true)
            html.required.set(true)
            txt.required.set(true)
            md.required.set(true)
        }
    }

    withType<DetektCreateBaselineTask> {
        jvmTarget = projectJvmTarget
    }

    withType<Test>().configureEach {
        jvmArgs = listOf(
            "-Dkotlintest.tags.exclude=Integration,EndToEnd,Performance",
        )
        testLogging {
            events("passed", "skipped", "failed")
        }
        testLogging.showStandardStreams = true
        useJUnitPlatform()
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = projectJvmTarget
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines)
    ktlint("com.pinterest:ktlint:0.46.1")

    testImplementation(libs.mockk)
    testImplementation(libs.junit)
}
