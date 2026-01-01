package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.GET
import io.github.kingg22.ktorgen.http.Path
import io.ktor.client.HttpClient

data class User(val id: Int, val name: String)

sealed interface ApiRoutes {
    val httpClient: HttpClient
    val basePath: String

    @GET
    suspend fun getInfo(): String
}

interface UserRoutes : ApiRoutes {
    @GET("{userId}")
    suspend fun getUser(@Path("userId") userId: Int): User
}
