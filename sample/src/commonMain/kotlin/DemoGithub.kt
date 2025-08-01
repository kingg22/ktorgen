package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.http.GET
import io.github.kingg22.ktorgen.http.Headers
import io.github.kingg22.ktorgen.http.Path

@KtorGen
interface DemoGithub {
    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(@Path owner: String, @Path repo: String): String

    @Headers("Content-Type: application/json")
    @GET("users")
    suspend fun getUsers(): List<String>
}
