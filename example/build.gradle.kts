import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
}

group = "io.github.kingg22"
version = libs.versions.ktorgen.version.get()

kotlin {
    compilerOptions {
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }

    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "$group.ktorgen.sample"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        packaging.resources.excludes += "/META-INF/*"
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }

    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }

    js {
        browser()
        nodejs()
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    // Tier 1
    // iOS
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    // macOs
    macosX64()
    macosArm64()
    // Tier 2
    // watchOS
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    // tvOS
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    // linux
    linuxX64()
    linuxArm64()
    // Tier 3
    // android native
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    // windows
    mingwX64()
    watchosDeviceArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.annotations)
        implementation(libs.ktor.client.core)
    }
}

dependencies {
    // ksp(projects.compiler) // KMP project don't use this, only jvm or android kotlin projects

    // don't apply on common main because we're going to generate on each platform. STILL FAILING
    // kspCommonMainMetadata(projects.compiler)
    add("kspAndroid", projects.compiler)
    add("kspJvm", projects.compiler)
    add("kspJs", projects.compiler)
    add("kspWasmJs", projects.compiler)
    add("kspAndroidNativeX64", projects.compiler)
    add("kspAndroidNativeX86", projects.compiler)
    add("kspAndroidNativeArm32", projects.compiler)
    add("kspAndroidNativeArm64", projects.compiler)
    add("kspLinuxX64", projects.compiler)
    add("kspLinuxArm64", projects.compiler)
    add("kspMingwX64", projects.compiler)
    add("kspIosX64", projects.compiler)
    add("kspIosArm64", projects.compiler)
    add("kspIosSimulatorArm64", projects.compiler)
    add("kspTvosArm64", projects.compiler)
    add("kspMacosArm64", projects.compiler)
    add("kspMacosX64", projects.compiler)
}

ksp {
    // pass argument to compiler
    arg("ktorgen_check_type", "0")
    arg("ktorgen_print_stacktrace_on_exception", "false")
}
