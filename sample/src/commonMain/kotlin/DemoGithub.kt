package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.http.GET
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.HeaderParam
import io.github.kingg22.ktorgen.http.Path
import io.ktor.client.request.HttpRequestBuilder

@KtorGen(optInAnnotations = [ExperimentalApi::class])
@Deprecated("")
@OptIn(ExperimentalApi::class)
interface DemoGithub : ApiServiceValid {
    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(@Path owner: String, @Path repo: String, @HeaderParam("Authorization") token: String): String

    @Header("Content-Type", "application/json")
    @GET("users")
    @Deprecated("")
    suspend fun getUsers(dynamic: HttpRequestBuilder.() -> Unit): List<String>
}
