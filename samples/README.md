# KtorGen samples

This is a subproject of KtorGen dedicated to showing samples code and integration tests.

All samples don't fetch or interact with any external service (real APIs).
These samples contribute to KtorGen test coverage.
The most *realistic* samples have tests with Ktor client mocking.

## Contributing

If you want to contribute, please open a PR with your sample.
Complex samples, realistic usages are welcome.

If you are migrating from another framework, like Retrofit, please add a link to your repository or commit changes.
The only requirement is that URL needs to be accessible from the public internet.

## Testing configuration

This is the main project (in terms of Gradle configuration and IDE first usage).
In `settings.gradle` you can find the `includeBuild` directive to include the main project as subproject.
You can see more about [composite builds of Gradle here](https://docs.gradle.org/current/userguide/composite_builds.html).

Multi-module projects != composite builds.
KtorGen is a multi-module project, and samples is a composite build that consumes the main project when it's not in a CI environment.

To make an integration and realistic test, these samples can override the version of Kotlin and/or Ktor used in examples.

To do that, you can use the `ktorgen_samples_ktor_version` and `ktorgen_samples_kotlin_version` properties as environment variables.

## Real migrations
_List of real migrations here:_
