@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalBCVApi::class, ExperimentalWasmDsl::class)

import com.android.build.api.dsl.androidLibrary
import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxBinaryCompatibility)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.kingg22"
version = libs.versions.ktorgen.version.get()

apiValidation {
    // temp disable until publish
    validationDisabled = true
    klib {
        enabled = true
    }
}

kotlin {
    compilerOptions {
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }

    /*
    // after kotlin 2.2.0
    abiValidation {
        enabled.set(true)
        filters {
            excluded {
                annotatedWith.add("$group.ktorgen.core.InternalKtorGen")
            }
        }
    }
     */

    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "$group.ktorgen.annotations"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }

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

    js {
        nodejs()
    }

    wasmWasi {
        nodejs()
    }

    // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    // Tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()
    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
}

ktlint {
    version.set(libs.versions.ktlint.pinterest.get())
}

dokka.dokkaSourceSets.configureEach {
    skipEmptyPackages = true
    skipDeprecated = false
    reportUndocumented = true
    enableJdkDocumentationLink = true
    enableKotlinStdLibDocumentationLink = true
}
