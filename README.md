# ⚡ ktorgen — Kotlin + KSP + Ktor Client Code Generator

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin_Multiplatform-%237F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](https://www.jetbrains.com/kotlin-multiplatform/)
[![Ktor Client](https://img.shields.io/badge/Ktor_Client-D93FD1.svg?style=flat-square&logo=ktor&logoColor=white&link=https%3A%2F%2Fktor.io%2F)](https://ktor.io/)

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.kingg22/ktorgen-annotations)](https://mvnrepository.com/artifact/io.github.kingg22/ktorgen-compiler)
[![Maven Central Last Update](https://img.shields.io/maven-central/last-update/io.github.kingg22/ktorgen-annotations)](https://mvnrepository.com/artifact/io.github.kingg22/ktorgen-compiler)

[![GitHub License](https://img.shields.io/github/license/kingg22/ktorgen)](https://github.com/kingg22/ktorgen/blob/main/LICENSE.txt)
[![GitHub last commit (branch)](https://img.shields.io/github/last-commit/kingg22/ktorgen/main)](https://github.com/kingg22/ktorgen/commits/main/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kingg22_ktorgen&metric=alert_status&token=958b0f9aa5f280c62c52a9f18026711a6df10759)](https://sonarcloud.io/summary/new_code?id=kingg22_ktorgen)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kingg22_ktorgen&metric=coverage)](https://sonarcloud.io/summary/new_code?id=kingg22_ktorgen)

Ktorgen is a 100% compile-time code generator for creating HTTP clients using Ktor Client and interface annotations, inspired by [Retrofit](https://github.com/square/retrofit) and [Ktorfit](https://github.com/Foso/Ktorfit).

### 📌 Features

🔹 No runtime dependencies — you only need the annotations and the compiler in your build.

🔹 100% compatible with Kotlin, Kotlin Multiplatform,
[KSP 2](https://github.com/google/ksp), and [Ktor Client](https://ktor.io/).

🔹 100% Ktor configuration — no unnecessary overhead or wrappers added.

🔹 Annotation retention: SOURCE, BINARY (_only for RequiresOptIn_).

🔹 Generated code annotated with `@Generated` and `@Suppress` to avoid warnings and exclude of analysis tools.

🔹 Support [suspend fun](https://kotlinlang.org/docs/async-programming.html#coroutines),
[Coroutines Flow](https://kotlinlang.org/docs/flow.html) and
[Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/) out of the box.

🔹 Optional type-safe headers, using `Headers.ContentTypes` or `Headers.Companion.*`

🔹 Optional inheritance between interfaces (use [delegation](https://kotlinlang.org/docs/delegation.html) to implement).

🔹 Experimental support for [vararg parameters](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs) and
[Pair](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-pair/) type.

📚 [More documentation of annotations](https://kingg22.github.io/ktorgen/).

## 📦 Instalación
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.kingg22/ktorgen-annotations)](https://mvnrepository.com/artifact/io.github.kingg22/ktorgen-compiler)

Install [KSP plugin](https://github.com/google/ksp)

Install [Ktor Client Core and an Engine](https://ktor.io/docs/client-create-new-application.html#add-dependencies)

* Version Catalog
```toml
[versions]
ktorgen = "<current-version>"

[libraries]
ktorgen-annotations = { group = "io.github.kingg22", name = "ktorgen-annotations", version.ref = "ktorgen" }
ktorgen-compiler = { group = "io.github.kingg22", name = "ktorgen-compiler", version.ref = "ktorgen" }
```

* Gradle for Kotlin JVM (KMP projects see below)
```kotlin
dependencies {
  implementation("io.github.kingg22:ktorgen-annotations:<current-version>")
  ksp("io.github.kingg22:ktorgen-compiler:<current-version>")
}
```

## 🔄 Comparison
| Feature                               | Retrofit                             | Ktorfit                        | ktorgen 🚀                                                     |
|---------------------------------------|--------------------------------------|--------------------------------|----------------------------------------------------------------|
| Based on Ktor Client                  | ❌                                    | ✅                              | ✅                                                              |
| Runtime dependencies                  | ✅ (Reflection, converters, adapters) | ✅ (Converts)                   | ❌                                                              |
| Retrofit-like annotations             | ✅ (Runtime retention)                | ✅ + ⚠️ more annotations        | ✅ with _smart use_ that reduces unnecessary annotations        |
| Type-safe headers                     | ❌                                    | ❌                              | ✅ with `Headers.ContentTypes` and `Headers.Companion.*`        |
| Cookie support                        | ⚠️ using the Header annotation       | ⚠️ Using the Header annotation | ✅ with `@Cookie`                                               |
| Using an `@Part` without `@Multipart` | ❌                                    | ❌                              | ✅ (smart use, your intended use is understood)                 |
| Customizing the generated code        | Limited                              | Limited                        | ✅ Using options in `@KtorGen` and `@KtorGenFunction`           |
| Synchronous request                   | ✅                                    | ✅                              | ❌ Ktor Client don't offer synchronous request, only for `Flow` |

**BREAKING CHANGES:**
- Since [PR #1](https://github.com/kingg22/ktorgen/pull/1), Header annotations have been different:

  Before this PR, the API of annotation is "identical" to Retrofit annotations.

  Before on functions `@Headers("Content-Type: application/json")` after `@Header("Content-Type", "application/json")`

  Before on parameters `@Header("Content-Type") param: String` after `@HeaderParam("Content-Type") param: String`

- Feature:

  <details>
  <summary> Added jetbrains annotations to provide IDE autocompletion and highlights. </summary>
  <br>
  <img src="docs/ide_pattern_matching.png" alt="ide_pattern_matching">
  <img src="docs/jetbrains_highlights.png" alt="jetbrains_highlights">
  </details>

  Now `@Header` is repeatable and type-safe because follow the format `name: value` parse it,
  problems with vararg empty is very annoying.

### Additional configuration for Kotlin Multiplatform projects with code in commonMain
*If you're coming from ktorfit, this configuration was applied by the ktorfit gradle plugin.*
```kotlin
kotlin {
  sourceSets.commonMain {
    // Tell KMP to compile KSP metadata
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    dependencies {
      implementation(libs.ktorgen.annotations) // ktorgen annotations
      implementation(libs.ktor.client.core) // ktor client core and any plugins you need
    }
  }
}

dependencies {
  // For KMP projects, use instead of ksp(...)
  kspCommonMainMetadata(libs.ktorgen.compiler)
}

ksp {
  // optional, additional configuration for KSP
  arg("ktorgen_check_type", "2")
  /*
  0: Disable error checking
  1: Check errors
  2: Convert errors into warnings (default)
  */
}

// Workaround for KSP in KMP
tasks.named("runKtlintCheckOverCommonMainSourceSet") {
    dependsOn("kspCommonMainKotlinMetadata")
}

tasks.matching { it.name != "kspCommonMainKotlinMetadata" && it.name.startsWith("ksp") }
    .configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }
```

## Samples
- [sample folder](https://github.com/kingg22/ktorgen/tree/main/sample)
- [deezer-client-kt](https://github.com/kingg22/deezer-client-kt)

- Demo source code

  ![Github api source code](docs/source_example.png)

  Demo generated code

  ![Github api generated code](docs/generated_example.png)

- Demo advanced usage

  ![Github api advanced usage](docs/advanced_source_example.png)

  Demo generated code

  ![Github api advanced usage](docs/generated_advanced_example.png)

### 🔁 Migrating from Ktorfit to ktorgen
Migrating is as simple as:

Changing annotation imports to `io.github.kingg22.ktorgen.annotations.*`

Comma-separated header annotations are now repeatable and type-safe.

Before:

```kotlin
@Headers("Content-Type: application/json", "Accept: application/json")
suspend fun request(@Header("Authentication") token: String): String
```

After:

```kotlin
@Header("Content-Type", "application/json")
@Header("Accept", "application/json")
suspend fun request(@HeaderParam("Authentication") token: String): String
```

Passing your own Ktor HttpClient to the implementations, like this: `fun UserRoute(client)` for `interface UserRoute` and generated `class _UserRouteImpl`.

Real-life migration example: [deezer-client-kt](https://github.com/kingg22/deezer-client-kt/commit/98e7ccc360dc62861c6e9030650f681a99cddceb)

## Advanced use ⚒️
* Global options (_see above in KMP configuration how to pass this options with KSP_)
  - `ktorgen_check_type`: The compiler validate code before generate (default: 2)

    0: Turn off all error related to checking

    1: Check for errors

    2: Turn errors into warnings
  - `ktorgen_print_stacktrace_on_exception`: Print stacktrace on exception using [Throwable#printStacktrace](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html#printStackTrace()) (default: false)
  - _More option soon..._

### Roadmap 🚀
- [ ] Add matrix compatibility test on CI (_Kotlin versions, KSP versions, Ktor Client versions_) to know the range of compatibility.
- [ ] Add test for Fragment annotation
- [ ] Add test for Cookie annotation
- [ ] Resolve knowable issues described in https://github.com/kingg22/ktorgen/pull/27
- [ ] Resolve issues related to unresolved references, multi round processing, unexpected errors.

## 📜 Disclaimer
This repository is a fork of Ktorfit and Retrofit annotations, with my own changes and additions.
It is not affiliated with Ktor, JetBrains, Kotlin, Ktorfit, or Retrofit.
Credits to their respective authors.

[License: Apache 2.0](https://github.com/kingg22/ktorgen/blob/main/LICENSE.txt), same as Retrofit and Ktorfit.
