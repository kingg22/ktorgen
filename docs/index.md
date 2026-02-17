---
hide:
  - navigation
---
# Welcome to KtorGen

For API documentation visit [dokka site](./api/index.html).

## Design Philosophy

Ktorgen is intentionally designed to stay **simple**, **debuggable**, and **close to [Ktor Client](https://ktor.io/docs/welcome.html) semantics**.

- Do **not** replace complex logic that should be implemented in Ktor’s request/response pipeline.
- Avoid adding converters, interceptors, or complex behaviors inside generated functions or runtime APIs.
- Keep the call chain transparent so debugging remains straightforward.

If you have a feature request — such as response mapping or simple converters —
please open an issue on the [GitHub repository](https://github.com/kingg22/ktorgen/issues/new)
describing your use case.

## Compatibility Matrix (Kotlin / KSP / Ktor)

It's expected a **new version** of Kotlin / KSP / Ktor will be going to **work fine** with the latest **KtorGen** version.
Don't be afraid to try it out!

> **Key context (previous summary):**
>
> * [**KtorGen** uses **Kotlin 2.2.20**](https://github.com/kingg22/ktorgen/blob/main/gradle/libs.versions.toml#L27)
> (aligned with [androidx](https://github.com/androidx/androidx/blob/androidx-main/gradle/libs.versions.toml#L55)).
> * The **JS/WASM** target is **strict** and sensitive to compilation incompatibilities.
> * **Ktor Client** supports **JS/WASM** from version 3.x**.
>
>     * Although versions < 3 might compile, **they are not recommended**
> (released in 2024–11 or earlier and outside the scope of KtorGen support).

---

| Kotlin | KSP          | Ktor  | Result    |
|--------|--------------|-------|-----------|
| 2.3.10 | 2.3.5        | 3.4.0 | ✅ Success |
| 2.3.0  | 2.3.5        | 3.4.0 | ✅ Success |
| 2.3.0  | 2.3.5        | 3.3.3 | ✅ Success |
| 2.3.0  | 2.3.4        | 3.3.3 | ✅ Success |
| 2.3.0  | 2.3.3        | 3.3.2 | ✅ Success |
| 2.3.0  | 2.3.2        | 3.3.1 | ✅ Success |
| 2.2.21 | 2.3.1        | 3.3.0 | ✅ Success |
| 2.2.21 | 2.2.21-2.0.4 | 3.2.3 | ✅ Success |
| 2.2.20 | 2.3.0        | 3.0.0 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.4 | 3.2.0 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.4 | 3.1.3 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.3 | 3.3.0 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.2 | 3.2.2 | ✅ Success |

## 📜 Disclaimer
This repository is a fork of [Ktorfit](https://foso.github.io/Ktorfit/) and [Retrofit](https://square.github.io/retrofit/) annotations,
with my own changes and additions.
It is not affiliated with Ktor, JetBrains, Kotlin, Ktorfit, or Retrofit.
Credits to their respective authors.

[License: Apache 2.0](https://github.com/kingg22/ktorgen/blob/main/LICENSE.txt), same as Retrofit and Ktorfit.
