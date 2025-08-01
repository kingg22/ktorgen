package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.*

interface ApiServiceValid {
    @GET("users")
    suspend fun getUsers(@Query("page") page: Int): List<Any>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Any

    @POST("users")
    suspend fun createUser(@Body user: Any): Any

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: Any): Any

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String)

    @PATCH("users/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body status: Any): Any

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun loginUser(@Field("username") username: String, @Field("password") password: String): Any

    @Multipart
    @POST("media/upload")
    suspend fun uploadFile(@Part("description") description: String, @Part file: Any): Any

    @HEAD("ping")
    suspend fun ping()

    @GET
    suspend fun dynamicUrl(@Url url: String): Any
}
