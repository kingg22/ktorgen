package io.github.kingg22.ktorgen.example

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenCompanionExtFactory
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.core.KtorGenHttpClientExtFactory
import io.github.kingg22.ktorgen.core.KtorGenKmpFactory
import io.github.kingg22.ktorgen.core.KtorGenVisibility
import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl
import io.ktor.client.HttpClient

@OptIn(KtorGenExperimental::class, ExperimentalApi::class)
@KtorGenKmpFactory
expect fun InternalApi(httpClient: HttpClient): InternalApi

@OptIn(KtorGenExperimental::class)
@KtorGen("AnApi")
@KtorGenVisibilityControl(visibilityModifier = KtorGenVisibility.INTERNAL)
// @KtorGenTopLevelFactory conflict overload
@KtorGenCompanionExtFactory
@KtorGenHttpClientExtFactory
interface InternalApi {
    val httpClient: HttpClient

    companion object
}
