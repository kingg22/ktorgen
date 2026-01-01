# Dokka External References

If you want to include links to [KtorGen annotations](https://kingg22.github.io/ktorgen/api/index.html), you can use the following links:

```kotlin
dokka {
  dokkaSourceSets.configureEach {
    externalDocumentationLink {
      register("ktorgen") {
        url("https://kingg22.github.io/ktorgen/")
        packageListUrl("https://kingg22.github.io/ktorgen/api/annotations/package-list")
      }
    }
  }
}
```

With this your dokka documentation will include links to KtorGen annotations. ✨

## Note
- Example documentation with external references: [Deezer Client API](https://kingg22.github.io/deezer-client-kt/)
and the [build.gradle.kts](https://github.com/kingg22/deezer-client-kt/blob/main/deezer-client-kt/build.gradle.kts#L121)

Thanks for your support! 🫶
