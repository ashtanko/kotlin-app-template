plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.diktat.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
}
