package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.sample.model.IssueData
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import kotlinx.coroutines.flow.Flow

interface ApiServiceValid {
    val httpClient: HttpClient
    val otraCosa: Boolean
    var valorCambiante: Boolean?
    val token: String

    @Cookie(
        name = "session_id",
        value = "abc123",
        maxAge = 3600,
        expiresTimestamp = 1735689600000, // 01/01/2025 00:00:00 GMT
        secure = true,
        httpOnly = true,
        extensions = [Cookie.PairString(Cookie.SameSite, Cookie.SameSites.Strict)],
    )
    @Cookie("session_id", "abc123")
    @GET("users")
    suspend fun getUsers(
        @Query("page", true) page: Int,
        @Cookie("session_id") @Cookie("session") session: Int,
    ): List<IssueData>

    @GET("users/{id}/{repo}")
    suspend fun getUserById(@Path("id") userId: String, @Path repo: String): IssueData

    @POST("users")
    suspend fun createUser(@Body user: IssueData): IssueData

    @Header(name = "Accept", value = Header.ContentTypes.Application.Json)
    @Header(name = Header.ContentType, value = Header.ContentTypes.Application.Json)
    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: IssueData): IssueData

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String, @HeaderParam(name = Header.Authorization) token: String)

    @PATCH("users/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body status: IssueData): IssueData

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun loginUser(@Field("username") username: String, @Field("password") password: String): IssueData

    @Multipart
    @POST("media/upload")
    suspend fun uploadFile(@Part("description") description: String = token, @Part file: PartData.FileItem): IssueData

    @HEAD("ping")
    suspend fun ping(@QueryName hola: String)

    @GET
    fun dynamicUrl(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @HeaderMap vararg others: Pair<String, String?>,
        @Part parts: List<PartData>,
    ): Flow<Result<IssueData>>

    @Fragment("header")
    suspend fun dynamicQuery(builder: HttpRequestBuilder.() -> Unit): Result<IssueData>

    @HTTP("TRACE", "media/download")
    fun dynamicQuery(builder: HttpRequestBuilder, @Tag tagValue: String): Flow<IssueData>

    suspend fun dynamicQueryWithoutBody(builder: HttpRequestBuilder.() -> Unit): HttpResponse
    suspend fun dynamicQueryWithoutResponse(builder: HttpRequestBuilder.() -> Unit)

    suspend fun dynamicRequest(builder: HttpRequest): Result<IssueData>
    suspend fun dynamicRequest(requestData: HttpRequestData): Result<IssueData>

    @GET
    suspend fun normalQueryWithoutResponseBody()
}
