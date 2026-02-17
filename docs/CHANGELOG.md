# Changelog

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

### Dependencies

## [0.7.0] - 2026-02-17

### Added

- Add support for `HttpStatement` and `HttpRequestBuilder` as return types for a function.
- Allow **non-suspending functions** to be used for `HttpStatement` and `HttpRequestBuilder` return types.
- Add a compatibility table between KtorGen, Kotlin, KSP, and Ktor Client versions. This table is generated automatically.
  This is not a mandatory rule for usage of KtorGen.
- Expand support of `@Fragment` to function parameters.
- Add suppress warnings related to namings; this reduces the warnings produced by the IDE when viewing the generated code.

### Changed

- **BREAKING CHANGE**: The parameter of `@HeaderParam` was renamed from _name_ to _value_
- Migrate from [_mkdocs shadcn theme_](https://asiffer.github.io/mkdocs-shadcn/) to
[_mkdocs material theme_](https://squidfunk.github.io/mkdocs-material/) documentation website.
- Migrate to [Zensical](https://zensical.org/) the documentation website.
- [internal] Move samples to a separate build project (Composite Build)
- [internal] Use `Sequences` instead of `List` for intermediate collections
- [internal] Use `LazyTheadSafe.None` for lazy properties, the processor is single-threaded

### Removed

- Remove unnecessary `@file:JvmMultifileClass` annotation on some annotations files.

### Fixed

- Correct handling of Sequences extracted from KSP symbols.
- Fix incorrect filters between source symbols and reduce unnecessary mappings.
- Prevent incorrect usage of `it` param on `this.url {}` block in generated code.
- Exclude / Forbidden `abstract, external and expect` modifiers.

### Dependencies

- [internal] chore(deps): update dependency org.sonarqube to v7.2.2.6593 by @renovate[bot]
- [internal] chore(deps): update astral-sh/setup-uv action to v7.2.0 by @renovate[bot]
- [internal] chore(deps): update agp to v9 (major) by @renovate[bot]
- [internal] chore(deps): update dependency com.vanniktech.maven.publish to v0.36.0 by @renovate[bot]
- [internal] fix(deps): update dependency zensical to v0.0.17 by @renovate[bot]
- [internal] chore(deps): update gradle to v9.3.0 by @renovate[bot]
- [internal] chore(deps): update dependency org.jetbrains.kotlinx.kover to v0.9.5 by @renovate[bot]
- [internal] chore(deps): update gradle to v9.3.1 by @renovate[bot]
- [internal] chore(deps): update gradle/actions action to v5.0.1 by @renovate[bot]
- [internal] chore(deps): update astral-sh/setup-uv action to v7.2.1 by @renovate[bot]
- [internal] fix(deps): update dependency zensical to v0.0.20 by @renovate[bot]
- [internal] chore(deps): update dependency org.jetbrains.kotlinx.kover to v0.9.6 by @renovate[bot]
- [internal] fix(deps): update dependency zensical to v0.0.21 by @renovate[bot]
- [internal] chore(deps): update agp to v9.0.1 by @renovate[bot]
- [internal] chore(deps): update dependency org.jetbrains.kotlinx.kover to v0.9.7 by @renovate[bot]
- [internal] chore(deps): update astral-sh/setup-uv action to v7.3.0 by @renovate[bot]
- [internal] fix(deps): update dependency zensical to v0.0.23 by @renovate[bot]

## [0.6.0] - 2025-12-17

### Added

- Add [wasmJs target](https://kotlinlang.org/docs/wasm-overview.html) on annotations. This target was missing from
[all supported targets](https://ktor.io/docs/client-supported-platforms.html) to match the ktor client core.
- Add warning _suspicious DOUBLE_SLASH_IN_URL_PATH_ when the url path contains double slashes caused by baseUrl + path.

### Changed

- update agp to v8.13.2 by @renovate[bot]
- ~~[internal] Add parametrized tests for different KSP versions (K1, K2)~~
- [internal] update softprops/action-gh-release action to v2.5.0 by @renovate[bot]
- [internal] update dependency org.jlleitschuh.gradle.ktlint to v14 by @renovate[bot]
- [internal] update dependency com.vanniktech.maven.publish to v0.35.0 by @renovate[bot]
- [internal] update dependency org.sonarqube to v7.2.1.6560 by @renovate[bot]
- [internal] update actions/checkout action to v6 by @renovate[bot]
- [internal] update astral-sh/setup-uv action to v7.1.6 by @renovate[bot]
- [internal] update gradle to v9.2.1 by @renovate[bot]
- [internal] update dependency androidx.room:room-compiler-processing-testing to v2.8.4 by @renovate[bot]
- [internal] update dependency org.jetbrains.changelog to v2.5.0 by @renovate[bot]
- [internal] update dependency org.jetbrains.kotlinx.kover to v0.9.4 by @renovate[bot]
- [internal] update actions/cache action to v5 by @renovate[bot]
- [internal] update actions/upload-artifact action to v6 by @renovate[bot]
- [internal] Update **KSP plugin** to 2.3.4
- [internal] Downgrade **ktor-client-core** to 3.3.1
- [internal] Update **klib api** file and **yarn lock** files

### Fixed

- [issue #48](https://github.com/kingg22/ktorgen/issues/48): [ERROR] Accessing symbol but the PSI changes KtorGenProcessor.onFinish
- [issue #55](https://github.com/kingg22/ktorgen/issues/55): [FALSE POSITIVE] URL checker for syntax error is raised in valid URL
- [issue #61](https://github.com/kingg22/ktorgen/issues/61) Add ProGuard consumer rules for Android target
- [issue #57](https://github.com/kingg22/ktorgen/issues/57): [FALSE POSITIVE]: A private class without accessors
is not valid when have an expect KMP function

### Security

- Remove dependency verification metadata because it adds an overhead to the development environment

## [0.5.1] - 2025-11-12

### Changed

- [Internal] Update **ktor-client-core** dependency to 3.3.2

### Fixed

- Fixed modifiers of interface don't include those in the generated class.
E.g. `sealed interface Foo` will generate `public class _FooImpl : Foo`. Instead of `public sealed class _FooImpl : Foo`.

## [0.5.1-RC] - 2025-11-12

### Changed

- [Internal] Update **ktor-client-core** dependency to 3.3.2

### Fixed

- Fixed modifiers of interface don't include those in the generated class.
E.g. `sealed interface Foo` will generate `public class _FooImpl : Foo`. Instead of `public sealed class _FooImpl : Foo`.

## [0.5.0] - 2025-11-11

### Added

- New **KMP support** for `expect/actual` functions:
  * Added `@KtorGenFunctionKmp` annotation and validation for `expect` modifier.
  * Added support to generate `actual` implementations automatically.
- Added **JetBrains Changelog plugin** for version tracking.
- Added **MkDocs documentation system** using Python and `uv`.
  * Added **GitHub Pages** automation for the new documentation website.
- Added **Markdown documentation files** describing configuration and setup.
  * Added integration of **Dokka output** with `docs/` to include in MkDocs site.
- Added **custom exceptions** for KSP processing to control flow instead of generic errors.
- Added **cookie validation**, tests, and mapping utilities.
- Added **SonarQube** integration and quality reporting workflows.
- Added new **tests** for generator, mapper, validator, and logging coverage.
- [Internal] Added **`@KtorGenWithoutCoverage`** annotation for coverage exclusions.
- [Internal] Added **diagnostic timers** with equality, hash, and readable formatting.
- [Internal] Added **logger enums** for strict check types and enhanced diagnostics.
- [Internal] Added **utility and helper extensions** for annotated KSP symbols.
- [Internal] Added **CODEOWNERS** file and CI rules for branch and PR builds.
- [Internal] Added **GPG attributes**, dependency locks, and verification keys in lenient mode.

### Changed

- [Internal] Major **refactor** of `KotlinpoetGenerator`:
  * Split constructor, expect function, factory function, member function and parameter annotation generation.
  * Handles imports via `MemberName`.
- Improved **generator, validator, and mapper** to use a `context parameter` for logging and timing.
- Reorganized **KotlinPoetGenerator** to be stateless and context-based.
- Improved **logger and diagnostics** system with graceful errors and detailed messages.
- [Internal] Moved `checkImplementation`, `timer.require`, `timer.requireNotNull` to a top-level function with overloads.
- Improved performance of **mapping phase** using `sequence` APIs instead of `toList`.
- Enhanced **error reporting** and logging consistency across phases.
- Improved **coverage, performance, and style** for compiler modules.
- Updated **Gradle**, **Kotlin**, **Dokka**, **Room**, **KSP**, and **Kover** dependencies.
- Adjusted **build configurations** for Kotlin 2.2.20 and KSP 2.2.20–2.0.4 builds.
- Enhanced **tests** for more scenarios: cookies, fragments, constructors, etc.
- Updated **actions and workflows** for artifact uploads and SonarQube analysis.

### Removed

- Removed **brief documentation** from the README — now redirected to GitHub Pages.
- [Internal] Removed macOS build workflow (unused).

### Fixed

- Fixed validation for cookies and parameter lists.
- Fixed incorrect invocation kind detection inside try-catch blocks.
- Fixed type mismatch after PSI changes on `ParameterBodyGenerator`.
- Fixed duplication in generator constants and imports.
- Fixed missing `FactoryFunctionKey` equality/hash in generator.
- Fixed a crash when mapper found an expected interface type.
- Fixed issues with KSP resolving and `ClassName` usage in generators.
- [Internal] Fixed minor KtLint and formatting issues.
- [Internal] Fixed `lazy` property misuse and `requireNotNull` handling.

### Security

- No direct security patches, but build pipelines now use **explicit action SHAs** and version-locked dependencies for safety.

[Unreleased]: https://github.com/kingg22/ktorgen/compare/0.7.0...HEAD
[0.7.0]: https://github.com/kingg22/ktorgen/compare/0.7.0-RC...0.7.0
[0.7.0-RC]: https://github.com/kingg22/ktorgen/compare/0.6.0...0.7.0-RC
[0.6.0]: https://github.com/kingg22/ktorgen/compare/0.5.1...0.6.0
[0.6.0-RC]: https://github.com/kingg22/ktorgen/compare/0.5.1...0.6.0-RC
[0.5.1]: https://github.com/kingg22/ktorgen/compare/0.5.1-RC...0.5.1
[0.5.1-RC]: https://github.com/kingg22/ktorgen/compare/0.5.0...0.5.1-RC
[0.5.0]: https://github.com/kingg22/ktorgen/commits/0.5.0
