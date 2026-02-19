package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.http.GET
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.HeaderParam
import io.github.kingg22.ktorgen.http.Path
import io.ktor.client.request.HttpRequestBuilder
import kotlin.jvm.JvmSynthetic

@OptIn(KtorGenExperimental::class)
@KtorGenAnnotationPropagation(optInAnnotations = [ExperimentalApi::class])
@ExperimentalApi
internal interface DemoGithub : ApiServiceValid {
    @GET("repos/{owner}/{repo}")
    @Header("Accept", "application/json")
    @Header(Header.Accept, Header.ContentTypes.Application.Cbor)
    @JvmSynthetic
    suspend fun getRepo(@Path owner: String, @Path repo: String, @HeaderParam("Authorization") token: String): String

    @Header("Content-Type", "application/json")
    @GET("users")
    @Deprecated("")
    suspend fun getUsers(dynamic: HttpRequestBuilder.() -> Unit): List<String>
}
