package io.github.kingg22.ktorgen.example

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.core.KtorGenFunctionKmp
import io.ktor.client.HttpClient

@OptIn(KtorGenExperimental::class, ExperimentalApi::class)
@KtorGenFunctionKmp
expect fun InternalApi(httpClient: HttpClient): InternalApi

@KtorGen(
    "AnApi",
    visibilityModifier = "internal",
    generateTopLevelFunction = false, // conflict overload
    generateCompanionExtFunction = true,
    generateHttpClientExtension = true,
)
interface InternalApi {
    val httpClient: HttpClient

    companion object
}
