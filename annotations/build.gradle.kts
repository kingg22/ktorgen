@file:OptIn(ExperimentalAbiValidation::class)

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
}

group = "io.github.kingg22"
version = libs.versions.ktorgen.version.get()

kotlin {
    compilerOptions {
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }

    abiValidation {
        enabled.set(true)
        filters {
            excluded {
                annotatedWith.add("$group.ktorgen.core.InternalKtorGen")
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    // tier 2
    linuxX64()
    linuxArm64()
    // tier 3
    mingwX64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
}

android {
    namespace = "$group.ktorgen"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ktlint {
    version.set(
        libs.versions.ktlint.pinterest
            .get(),
    )
}

dokka.dokkaSourceSets.configureEach {
    skipEmptyPackages = true
    skipDeprecated = false
    reportUndocumented = true
    enableJdkDocumentationLink = true
    enableKotlinStdLibDocumentationLink = true
}

kover {
    reports.total {
        verify {
            rule("Basic Line Coverage") {
                minBound(60, CoverageUnit.LINE)
            }

            rule("Basic Branch Coverage") {
                minBound(20, CoverageUnit.BRANCH)
            }
        }
    }
}
