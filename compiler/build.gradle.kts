import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.kingg22"
version = libs.versions.ktorgen.version.get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(projects.annotations)
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poet.ksp)

    testImplementation(kotlin("test"))
}

ktlint {
    version.set(
        libs.versions.ktlint.pinterest
            .get(),
    )
}

kover {
    reports.total {
        verify {
            // temporal disable
            rule("Basic Line Coverage") {
                disabled.set(true)
                minBound(60, CoverageUnit.LINE)
            }

            rule("Basic Branch Coverage") {
                disabled.set(true)
                minBound(20, CoverageUnit.BRANCH)
            }
        }
    }
}
