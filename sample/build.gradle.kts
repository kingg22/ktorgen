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

    // TODO add a android app to samples
    androidLibrary {
        namespace = "$group.ktorgen.sample"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        packaging.resources.excludes.add("/META-INF/*")
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
    // ksp(projects.compiler) // a KMP project doesn't use this, only jvm or android kotlin projects
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

ksp {
    // pass argument to compiler
    arg("ktorgen_check_type", "0")
    arg("ktorgen_print_stacktrace_on_exception", "false")
}

tasks.named("runKtlintCheckOverCommonMainSourceSet") {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Workaround kotlin multiplatform with ksp
tasks.matching { it.name != "kspCommonMainKotlinMetadata" && it.name.startsWith("ksp") }
    .configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }
