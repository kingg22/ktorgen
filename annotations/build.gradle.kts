import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.kingg22"
description = "Kotlin annotations for generating Ktor Client interface implementations using the KtorGen KSP processor."
version = libs.versions.ktorgen.version.get()

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        klib {
            enabled.set(true)
            keepUnsupportedTargets.set(true)
        }
    }

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

    sourceSets.commonMain.dependencies {
        implementation(libs.jetbrains.annotations)
    }
}

dokka.dokkaSourceSets.configureEach {
    skipEmptyPackages.set(true)
    skipDeprecated.set(false)
    reportUndocumented.set(true)
    enableJdkDocumentationLink.set(true)
    enableKotlinStdLibDocumentationLink.set(true)
    suppressGeneratedFiles.set(true)
}

tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "ktorgen-annotations", version.toString())

    pom {
        name = "KtorGen - Annotations"
        description = project.description
        inceptionYear = "2025"
        url = "https://github.com/kingg22/ktorgen"
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "kingg22"
                name = "Rey Acosta (Kingg22)"
                url = "https://github.com/kingg22"
            }
        }
        scm {
            url = "https://github.com/kingg22/ktorgen"
            connection = "scm:git:git://github.com/kingg22/ktorgen.git"
            developerConnection = "scm:git:ssh://git@github.com/kingg22/ktorgen.git"
        }
    }
}
