# Welcome to KtorGen

For API documentation visit [dokka site](./api/index.html).

## Design Philosophy

Ktorgen is intentionally designed to stay **simple**, **debuggable**, and **close to Ktor Client semantics**.

- Do **not** replace complex logic that should be implemented in Ktor’s request/response pipeline.
- Avoid adding converters, interceptors, or complex behaviors inside generated functions or runtime APIs.
- Keep the call chain transparent so debugging remains straightforward.

If you have a feature request — such as response mapping or simple converters —
please open an issue on the [GitHub repository](https://github.com/kingg22/ktorgen/issues/new)
describing your use case.

## 📜 Disclaimer
This repository is a fork of Ktorfit and Retrofit annotations, with my own changes and additions.
It is not affiliated with Ktor, JetBrains, Kotlin, Ktorfit, or Retrofit.
Credits to their respective authors.

[License: Apache 2.0](https://github.com/kingg22/ktorgen/blob/main/LICENSE.txt), same as Retrofit and Ktorfit.
