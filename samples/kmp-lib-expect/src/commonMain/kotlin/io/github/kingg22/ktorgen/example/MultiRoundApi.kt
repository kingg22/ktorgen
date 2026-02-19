package io.github.kingg22.ktorgen.example

import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.core.KtorGenKmpFactory
import io.github.kingg22.ktorgen.core.KtorGenVisibility
import io.github.kingg22.ktorgen.core.KtorGenVisibilityControl
import io.github.kingg22.ktorgen.http.Body
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.POST
import io.github.kingg22.ktorgen.http.Query
import io.ktor.client.HttpClient
import kotlin.jvm.JvmSynthetic

@OptIn(KtorGenExperimental::class, ExperimentalApi::class)
@KtorGenKmpFactory
internal expect fun MultiRoundApi(httpClient: HttpClient): MultiRoundApi

@OptIn(KtorGenExperimental::class)
@KtorGenVisibilityControl(classVisibilityModifier = KtorGenVisibility.PRIVATE)
@KtorGenAnnotationPropagation(
    factoryFunctionAnnotations = [JvmSynthetic::class],
    optInAnnotations = [ExperimentalApi::class],
)
@ExperimentalApi
internal interface MultiRoundApi {
    @POST
    @Header("Content-Type", "application/json")
    suspend fun getAlbumDiscography(
        @Body jsonBody: String,
        @Query("api_token") apiToken: String = "",
        @Query("method") method: String = "album.getDiscography",
        @Query("api_version") apiVersion: Float = 1.0f,
        @Query input: Int = 3,
    ): String

    @POST
    @Header("Content-Type", "application/json")
    suspend fun getAlbumData(
        @Body jsonBody: String,
        @Query("api_token") apiToken: String = "",
        @Query("method") method: String = "album.getData",
        @Query("api_version") apiVersion: Float = 1.0f,
        @Query input: Int = 3,
    ): String
}
