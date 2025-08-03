package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.sample.model.IssueData
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.content.PartData

interface ApiServiceValid {
    val httpClient: HttpClient
    val otraCosa: Boolean
    var valorCambiante: Boolean

    @GET("users")
    suspend fun getUsers(@Query("page") page: Int): List<IssueData>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): IssueData

    @POST("users")
    suspend fun createUser(@Body user: IssueData): IssueData

    @Headers("Content-Type: application/json")
    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: IssueData): IssueData

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String)

    @PATCH("users/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body status: IssueData): IssueData

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun loginUser(@Field("username") username: String, @Field("password") password: String): IssueData

    @Multipart
    @POST("media/upload")
    suspend fun uploadFile(@Part("description") description: String, @Part file: PartData.FileItem): IssueData

    @HEAD("ping")
    suspend fun ping(@QueryName hola: String)

    @GET
    suspend fun dynamicUrl(@Url url: String): IssueData

    @GET
    suspend fun dynamicQuery(builder: HttpRequestBuilder.() -> Unit): IssueData

    @POST
    suspend fun dynamicQuery(builder: HttpRequestBuilder): IssueData
}
