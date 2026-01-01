---
hide:
  - navigation
---
## Configuration

This document explains how to configure **Ktorgen** in your project using **Gradle**, both for **JVM/Android** and **Kotlin Multiplatform (KMP)** targets.
The goal is to ensure the KSP (Kotlin Symbol Processing) plugin correctly generates client code from your annotations, regardless of the target platform.

---

## 📦 Basic Setup

Ktorgen consists of two main components:

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.kingg22/ktorgen-annotations)](https://mvnrepository.com/artifact/io.github.kingg22/ktorgen-compiler)

```kotlin
implementation("io.github.kingg22:ktorgen-annotations:<current-version>")
ksp("io.github.kingg22:ktorgen-compiler:<current-version>")
```

- `ktorgen-annotations` → annotations that you apply in your code.
- `ktorgen-compiler` → KSP processor that generates the corresponding Ktor client implementations.

In any setup, you’ll need to include both modules, plus your chosen **Ktor client engine**.

Install [KSP plugin](https://github.com/google/ksp)

Install [Ktor Client Core and an Engine](https://ktor.io/docs/client-create-new-application.html#add-dependencies)

### Example — Common Dependencies

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation(libs.ktorgen.annotations)
            implementation(libs.ktor.client.core)
            // optionally:
            // implementation(libs.ktor.client.content.negotiation)
            // implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
```

---

## ⚙️ Configuration for JVM / Android Projects

This is the **recommended default configuration** for non-multiplatform projects.
KSP will process your annotations and generate all source code under the standard `build/generated/ksp/...` directory.

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.ktorgen.annotations)
    implementation(libs.ktor.client.core)

    // Apply the KSP processor
    ksp(libs.ktorgen.compiler)
}
```

That’s it — your generated clients will appear in:
`build/generated/ksp/main/kotlin/`

---

## 🌐 Configuration for Kotlin Multiplatform (KMP)

KSP does not yet generate code directly in the `commonMain` source set.
This requires a **manual workaround** to make the generated metadata available to the `commonMain` compilation phase.

There are **two valid configurations** for KMP:

---

### 🧩 Option 1 — Workaround for Common Metadata

This approach makes KSP generate sources under `metadata/commonMain` and manually includes them in the source set.

> ✅ Recommended when your Ktor clients are defined in `commonMain` and shared across multiple platforms.

```kotlin
kotlin {
    // multiplatform configuration ...
    sourceSets {
        commonMain {
            // Include generated KSP metadata for KMP
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.ktorgen.annotations)
                implementation(libs.ktor.client.core)
            }
        }
    }
}

dependencies {
    // Apply processor at metadata level
    kspCommonMainMetadata(libs.ktorgen.compiler)
}

// Ensure KSP runs before all platform-specific tasks
tasks.matching { it.name != "kspCommonMainKotlinMetadata" && it.name.startsWith("ksp") }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Fix dependency ordering between KSP and other tasks (like lint)
tasks.named("runKtlintCheckOverCommonMainSourceSet") {
  dependsOn("kspCommonMainKotlinMetadata")
}
```

#### Notes

- This ensures code generated from `commonMain` annotations is visible to all platform targets.
- Required until **Kotlin 2.3.0** finalizes and stabilizes the new API for registering generated sources.
  See: [New API for Generated Sources](https://kotlinlang.org/docs/gradle-configure-project.html#register-generated-sources)

    * In Kotlin >= 2.3.0-Beta2 you must use `generatedKotlin.srcDir("...")`.

---

### ⚙️ Option 2 — Default (Per-Platform KSP Configuration)

The **recommended and most stable setup** for KMP right now.
Each platform runs its own KSP processor, generating its corresponding client implementation.

> ✅ Use this if your generated code includes platform-specific declarations or `expect`/`actual` functions.

```kotlin
kotlin {
    sourceSets {
      commonMain {
        dependencies {
            implementation(libs.ktorgen.annotations)
            implementation(libs.ktor.client.core)
        }
      }
    }
}

dependencies {
    // Do NOT apply KSP to commonMain (each platform will generate separately)
    add("kspAndroidDebug", libs.ktorgen.compiler)
    add("kspAndroidRelease", libs.ktorgen.compiler)
    add("kspJvm", libs.ktorgen.compiler)
    add("kspJs", libs.ktorgen.compiler)
    add("kspLinuxX64", libs.ktorgen.compiler)
    add("kspLinuxArm64", libs.ktorgen.compiler)
    add("kspMingwX64", libs.ktorgen.compiler)
    add("kspIosX64", libs.ktorgen.compiler)
    add("kspIosArm64", libs.ktorgen.compiler)
    add("kspIosSimulatorArm64", libs.ktorgen.compiler)
    add("kspTvosArm64", libs.ktorgen.compiler)
    add("kspMacosArm64", libs.ktorgen.compiler)
    add("kspMacosX64", libs.ktorgen.compiler)
    // ... each target required apply ksp and the processor
}
```

#### Notes

- Generated code will live in each platform’s own directory under:
`build/generated/ksp/<target>/kotlin/`
- Compilation will automatically pick the corresponding sources when building for each target.

---

## ⚠️ Common Issues

### ❗ Unresolved References

If you encounter errors like:

`Found unresolved symbols on finish round: [MyApi => [kotlin.jvm.JvmOverloads]]`


Ensure that:

1. The generated sources directory is correctly added to `kotlin.srcDir(...)`. In KMP project with option 1.
2. KSP tasks run **before** compilation. In KMP projects with option 2, remember add KSP to all targets you have.
3. You’re not using annotations such as `@JvmOverloads` or `@JvmStatic` in platform code that isn’t JVM-only.

> 🧠 **Tip:**
> `@JvmOverloads` and similar JVM annotations are only supported in `commonMain`, `jvmMain`, or `androidMain`.
> If you apply them in multiplatform configurations per source target, non-JVM targets will fail to compile due to missing expect/actual declarations.

---

## ✅ Summary

| Configuration                      | Description                                                      | Recommended for                                       |
|------------------------------------|------------------------------------------------------------------|-------------------------------------------------------|
| **Option 1** – Metadata Workaround | Generates code under `commonMain` using manual source inclusion. | Shared clients in common code.                        |
| **Option 2** – Per-Platform KSP    | Runs KSP independently on each target.                           | Platform-specific clients or expect/actual factories. |
