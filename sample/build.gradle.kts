@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Can't use android multiplatform library. See https://github.com/google/ksp/issues/2476
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

group = "io.github.kingg22"
version = libs.versions.ktorgen.version.get()

kotlin {
    compilerOptions {
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }

    applyDefaultHierarchyTemplate()

    // TODO add a android app to samples
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

    js {
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

    sourceSets.commonMain {
        // indicate to KMP plugin compile the metadata of ksp
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        dependencies {
            implementation(projects.annotations)
            implementation(libs.ktor.client.core)
        }
    }
}

dependencies {
    // ksp(projects.compiler) // KMP project don't use this, only jvm or android kotlin projects
    // apply on common main because in there is my code
    kspCommonMainMetadata(projects.compiler)
    /*
    // If I have more code with annotations on each target, apply on each target ksp
    add("kspJvm", projects.compiler)
    // add("kspJvmTest", projects.compiler)
    add("kspJs", projects.compiler)
    // add("kspJsTest", projects.compiler)
    add("kspAndroidNativeX64", projects.compiler)
    // add("kspAndroidNativeX64Test", projects.compiler)
    add("kspAndroidNativeArm64", projects.compiler)
    // add("kspAndroidNativeArm64Test", projects.compiler)
    add("kspLinuxX64", projects.compiler)
    // add("kspLinuxX64Test", project(":test-processor"))
    add("kspMingwX64", projects.compiler)
    // add("kspMingwX64Test", project(":test-processor"))
     */
}

android {
    namespace = "$group.ktorgen.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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
    version.set(libs.versions.ktlint.pinterest.get())
}

ksp {
    // pass argument to compiler
    arg("ktorgen_check_type", "2")
}

tasks.named("runKtlintCheckOverCommonMainSourceSet") {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Workaround kotlin multiplatform with ksp
tasks.matching { it.name != "kspCommonMainKotlinMetadata" && it.name.startsWith("ksp") }
    .configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }
