/*
 * Convention plugin shared by every pure-Kotlin/JVM subproject (`app`, `core`, ...).
 *
 * Bundles the Kotlin/JVM toolchain, the static-analysis stack (detekt/ktlint/diktat), Jacoco
 * instrumentation, and the JUnit 5 test stack that used to live inline in the single-module
 * root `build.gradle.kts`. See CLAUDE.md ("Versions are centralized") for the reasoning behind
 * keeping tool versions in `gradle/libs.versions.toml` rather than here.
 */
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm")
    jacoco
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.saveourtool.diktat")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
}

val projectJvmTarget = 17

kotlin {
    jvmToolchain(projectJvmTarget)
    compilerOptions {
        apiVersion.set(KOTLIN_2_2)
        languageVersion.set(KOTLIN_2_2)
    }
}

jacoco {
    toolVersion = "0.8.15"
}

// No shared `kover { reports { verify { ... } } }` rule here: the >=80% line-coverage bound is
// only meaningful for modules with actual business logic to exercise. `core` opts in (see
// core/build.gradle.kts); `app` is bootstrap/wiring code with nothing worth unit-testing, so it
// keeps Kover's reporting (koverXmlReport/koverHtmlReport still work) without the verify gate.

configure<KtlintExtension> {
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(true)
    enableExperimentalRules.set(true)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)
        reporter(ReporterType.HTML)
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

diktat {
    inputs {
        include("src/main/**/*.kt")
        exclude("**/generated/**")
    }
}

detekt {
    // Shared root config/baseline: the baseline is currently empty, so every module is held to
    // the same bar. Split it per module (config/detekt/<module>-baseline.xml) if debt diverges.
    baseline = rootProject.file("config/detekt/detekt-baseline.xml")
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        maxParallelForks = 1
        jvmArgs(
            "--add-opens",
            "java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-exports",
            "java.base/jdk.internal.util=ALL-UNNAMED",
            "--add-exports",
            "java.base/sun.security.action=ALL-UNNAMED",
            "-Dkotlintest.tags.exclude=Integration,EndToEnd,Performance",
        )
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
        finalizedBy(named("jacocoTestReport"))
    }

    named<JacocoReport>("jacocoTestReport") {
        dependsOn(named("test"))
        reports {
            listOf(html, xml, csv).forEach { it.required.set(true) }
        }
    }

    named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        violationRules {
            rule {
                limit {
                    minimum = "0.5".toBigDecimal()
                }
            }
        }
    }

    withType<Detekt>().configureEach {
        description = "Runs over this module's code base."
        parallel = true
        jvmTarget = "$projectJvmTarget"
        setSource(files("src/main/kotlin", "src/test/kotlin"))
        setOf(
            "**/*.kt",
            "**/*.kts",
            ".*/resources/.*",
            ".*/build/.*",
        ).forEach { include(it) }
        reports {
            listOf(xml, html, txt, md).forEach { it.required.set(true) }
        }
    }

    withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = "$projectJvmTarget"
    }
}

val libs = the<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(kotlin("stdlib"))

    "testImplementation"(libs.findLibrary("junit-api").get())
    "testImplementation"(libs.findLibrary("junit-params").get())
    "testRuntimeOnly"(libs.findLibrary("junit-engine").get())
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    "testImplementation"(libs.findLibrary("kotlin-test").get())
    "testImplementation"(libs.findLibrary("assertj").get())
    "testImplementation"(libs.findLibrary("mockk").get())
    "testImplementation"(libs.findLibrary("mockk-bdd").get())
    "testImplementation"(libs.findLibrary("mockito").get())
    "testImplementation"(libs.findLibrary("mockito-kotlin").get())
}
