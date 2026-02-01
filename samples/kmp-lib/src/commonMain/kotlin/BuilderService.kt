package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.Body
import io.github.kingg22.ktorgen.http.DELETE
import io.github.kingg22.ktorgen.http.Field
import io.github.kingg22.ktorgen.http.FormUrlEncoded
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.HeaderParam
import io.github.kingg22.ktorgen.http.Multipart
import io.github.kingg22.ktorgen.http.POST
import io.github.kingg22.ktorgen.http.PUT
import io.github.kingg22.ktorgen.http.Part
import io.github.kingg22.ktorgen.http.Path
import io.github.kingg22.ktorgen.sample.model.IssueData
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpStatement
import io.ktor.http.content.PartData

interface BuilderService {
    @Header(name = "Accept", value = Header.ContentTypes.Application.Json)
    @Header(name = Header.ContentType, value = Header.ContentTypes.Application.Json)
    @PUT("users/{id}")
    fun updateUser(@Path("id") id: String, @Body user: IssueData): HttpRequestBuilder

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String, @HeaderParam(Header.Authorization) token: String): HttpRequestBuilder

    @FormUrlEncoded
    @POST("auth/token")
    fun loginUser(@Field("username") username: String, @Field("password") password: String): HttpStatement

    @Multipart
    @POST("media/upload")
    suspend fun uploadFile(@Part description: String, @Part file: PartData.FileItem): HttpStatement

    @Header(name = "Accept", value = Header.ContentTypes.Application.Json)
    @Header(name = Header.ContentType, value = Header.ContentTypes.Application.Json)
    @PUT("users/{id}")
    fun updateCatching(@Path("id") id: String, @Body user: IssueData): Result<HttpRequestBuilder>

    @DELETE("users/{id}")
    suspend fun deleteSecure(
        @Path("id") id: String,
        @HeaderParam(Header.Authorization) token: String,
    ): Result<HttpRequestBuilder>

    @FormUrlEncoded
    @POST("auth/token")
    fun loginCatching(@Field("username") username: String, @Field("password") password: String): Result<HttpStatement>

    @Multipart
    @POST("media/upload")
    suspend fun uploadFileSecure(@Part description: String, @Part file: PartData.FileItem): Result<HttpStatement>
}
