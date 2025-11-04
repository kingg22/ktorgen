package io.github.kingg22.ktorgen.example

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.http.Body
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.POST
import io.github.kingg22.ktorgen.http.Query

@KtorGen(
    classVisibilityModifier = "private",
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
