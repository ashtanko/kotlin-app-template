/*
 * Copyright 2026 Oleksii Shtanko
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
plugins {
    id("template.kotlin-library")
    alias(libs.plugins.pitest)
}

kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
        val satisfyingNumberOfCores = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
        jvmArgs.set(listOf("-Xmx2048m"))
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("dev.shtanko.template.*"))
        targetTests.set(setOf("dev.shtanko.template.*"))
        pitestVersion.set(libs.versions.pitest.get())
        verbose.set(true)
        timestampedReports.set(false)
        threads.set(System.getenv("PITEST_THREADS")?.toInt() ?: satisfyingNumberOfCores)
        outputFormats.set(setOf("XML", "HTML"))
        junit5PluginVersion.set("1.2.1")
    }
}



dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines)

    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.turbine)
}
