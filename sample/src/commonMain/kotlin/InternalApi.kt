package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.core.KtorGen
import io.ktor.client.HttpClient

@KtorGen(
    "AnApi",
    visibilityModifier = "internal",
    generateTopLevelFunction = true,
    generateCompanionExtFunction = true,
    generateHttpClientExtension = true,
)
interface InternalApi {
    val httpClient: HttpClient

    companion object
}
