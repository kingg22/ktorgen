# Changelog

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

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

[Unreleased]: https://github.com/kingg22/ktorgen/compare/0.5.1-RC...HEAD
[0.5.1-RC]: https://github.com/kingg22/ktorgen/compare/0.5.0...0.5.1-RC
[0.5.0]: https://github.com/kingg22/ktorgen/commits/0.5.0
