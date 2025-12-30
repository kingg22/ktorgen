package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenFunction
import io.github.kingg22.ktorgen.http.*
import kotlin.jvm.JvmSynthetic

@KtorGen(
    classVisibilityModifier = "private",
    functionAnnotations = [JvmSynthetic::class],
    optInAnnotations = [ExperimentalApi::class],
)
@ExperimentalApi
internal interface MultiRoundApi {
    @POST
    @Header("Content-Type", "application/json")
    @KtorGenFunction(annotations = [JvmSynthetic::class])
    @JvmSynthetic
    suspend fun getAlbumDiscography(
        @Body jsonBody: String,
        @Query("api_token") apiToken: String = "",
        @Query("method") method: String = "album.getDiscography",
        @Query("api_version") apiVersion: Float = 1.0f,
        @Query input: Int = 3,
    ): String

    @POST
    @Header("Content-Type", "application/json")
    @KtorGenFunction(annotations = [JvmSynthetic::class])
    suspend fun getAlbumData(
        @Body jsonBody: String,
        @Query("api_token") apiToken: String = "",
        @Query("method") method: String = "album.getData",
        @Query("api_version") apiVersion: Float = 1.0f,
        @Query input: Int = 3,
    ): String
}
