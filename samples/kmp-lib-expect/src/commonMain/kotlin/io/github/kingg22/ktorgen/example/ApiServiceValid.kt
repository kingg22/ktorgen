package io.github.kingg22.ktorgen.example

import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.core.KtorGenFunctionKmp
import io.github.kingg22.ktorgen.example.model.IssueData
import io.github.kingg22.ktorgen.http.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.content.*
import kotlinx.coroutines.flow.Flow

@OptIn(KtorGenExperimental::class)
@KtorGenFunctionKmp
expect fun ApiServiceValid(httpClient: HttpClient, otraCosa: Boolean, valorCambiante: Boolean): ApiServiceValid

@KtorGen(
    generateTopLevelFunction = true,
    generateCompanionExtFunction = true,
    generateHttpClientExtension = true,
    classVisibilityModifier = "private",
)
interface ApiServiceValid {
    val httpClient: HttpClient
    val otraCosa: Boolean
    var valorCambiante: Boolean

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
    suspend fun uploadFile(@Part("description") description: String, @Part file: PartData.FileItem): IssueData

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

    companion object
}
