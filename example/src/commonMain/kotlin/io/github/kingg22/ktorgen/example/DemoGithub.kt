package io.github.kingg22.ktorgen.example

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.core.KtorGenFunctionKmp
import io.github.kingg22.ktorgen.http.GET
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.HeaderParam
import io.github.kingg22.ktorgen.http.Path
import io.ktor.client.HttpClient
import io.ktor.client.request.*

@Suppress("ktlint:standard:function-naming")
@OptIn(KtorGenExperimental::class, ExperimentalApi::class)
@KtorGenFunctionKmp
internal expect fun DemoGithub(httpClient: HttpClient, apiServiceValid: ApiServiceValid): DemoGithub

@KtorGen(optInAnnotations = [ExperimentalApi::class])
@ExperimentalApi
internal interface DemoGithub : ApiServiceValid {
    @GET("repos/{owner}/{repo}")
    @Header("Accept", "application/json")
    @Header(Header.Accept, Header.ContentTypes.Application.Cbor)
    suspend fun getRepo(@Path owner: String, @Path repo: String, @HeaderParam("Authorization") token: String): String

    @Header("Content-Type", "application/json")
    @GET("users")
    @Deprecated("")
    suspend fun getUsers(dynamic: HttpRequestBuilder.() -> Unit): List<String>
}
