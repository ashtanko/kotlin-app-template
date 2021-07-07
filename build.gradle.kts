import com.diffplug.gradle.spotless.SpotlessPlugin
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

val projectJvmTarget = "1.8"
val satisfyingNumberOfCores = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
val ktlint: Configuration by configurations.creating

plugins {
    kotlin("jvm") version "1.5.20"
    java
    jacoco
    idea
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    id("io.gitlab.arturbosch.detekt") version Versions.DETEKT
    id("org.jetbrains.dokka") version "1.4.32"
    id("com.diffplug.gradle.spotless") version "3.26.1"
    id("com.autonomousapps.dependency-analysis") version "0.58.0"
    id("info.solidsoft.pitest") version "1.5.1"
    kotlin("plugin.serialization") version "1.5.0"
    kotlin("kapt") version "1.5.20"
    id("org.sonarqube") version "3.3"
}

sonarqube {
    properties {
        property("sonar.projectKey", "ashtanko_kotlin-app-template")
        property("sonar.organization", "ashtanko")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

jacoco {
    toolVersion = "0.8.7"
}

buildscript {
    repositories {
        mavenCentral()
        jcenter()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
    }
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.COROUTINES}")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("io.reactivex.rxjava3:rxjava:${Versions.RX_JAVA}")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    implementation("org.jetbrains.kotlinx:lincheck:${Versions.LINCHECK}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    ktlint("com.pinterest:ktlint:0.41.0")

    implementation("com.google.dagger:dagger:${Versions.DAGGER}")
    kapt("com.google.dagger:dagger-compiler:${Versions.DAGGER}")
    kapt("com.google.auto.value:auto-value:1.8.1")
    implementation("com.google.auto.value:auto-value-annotations:1.8.1")

    testImplementation("org.jetbrains.kotlinx:lincheck:${Versions.LINCHECK}")
    testApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")

    testImplementation("io.kotlintest:kotlintest-core:3.4.2")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testImplementation("io.kotlintest:kotlintest-assertions-arrow:3.4.2")
    testImplementation("io.arrow-kt:arrow-core-test:0.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.assertj:assertj-core:3.18.1")
    testImplementation("org.mockito:mockito-core:3.6.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("ch.qos.logback:logback-core:1.2.3")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("io.reactivex.rxjava3:rxjava:${Versions.RX_JAVA}")
    testImplementation("com.carrotsearch:junit-benchmarks:0.7.0")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.SPEK}") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.SPEK}") {
        exclude(group = "org.jetbrains.kotlin")
    }
    // spek requires kotlin-reflect, can be omitted if already in the classpath
    testRuntimeOnly(kotlin("reflect"))

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.DETEKT}")
}

repositories {
    google()
    jcenter()
}

allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }
}

jacoco {

}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlintCheck by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F", "src/**/*.kt")
}

plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
        jvmArgs.set(listOf("-Xmx1024m"))
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("dev.shtanko.template.*"))
        targetTests.set(setOf("dev.shtanko.template.*"))
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
    //apply<KtlintPlugin>()
    apply<SpotlessPlugin>()
}

fun isLinux(): Boolean {
    val osName = System.getProperty("os.name").toLowerCase(Locale.ROOT)
    return listOf("linux", "mac os", "macos").contains(osName)
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
        }
        executionData(file("build/jacoco/test.exec"))
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = projectJvmTarget
            freeCompilerArgs = freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental"
        }
    }

    withType<Test>().configureEach {
        jvmArgs = listOf(
            "-Dkotlintest.tags.exclude=Integration,EndToEnd,Performance"
        )
        useJUnitPlatform {
            includeEngines("spek2", "junit-jupiter")
        }
        testLogging {
            events("passed", "skipped", "failed")
        }
        testLogging.showStandardStreams = true
        useJUnitPlatform()
    }

    withType<Detekt>().configureEach {
        jvmTarget = projectJvmTarget
    }

    withType<Detekt> {
        description = "Runs over whole code base without the starting overhead for each module."
        parallel = true
        baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
        config.from(file("config/detekt/detekt.yml"))

        include("**/*.kt")
        include("**/*.kts")
        exclude(".*/resources/.*")
        exclude(".*/build/.*")
        exclude("/versions.gradle.kts")
        exclude("buildSrc/settings.gradle.kts")

        reports {
            xml.enabled = true
            xml.destination = file("build/reports/detekt/detekt.xml")
            html.enabled = true
            txt.enabled = true
        }
    }

    withType<Test> {
        maxParallelForks = satisfyingNumberOfCores
    }

    // config JVM target to 1.8 for kotlin compilation tasks
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = projectJvmTarget
    }
}
