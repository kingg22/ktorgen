package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.*

interface ApiServiceWithWarnings {
    // ⚠️ @Path no usado en URL
    @GET("users")
    suspend fun getUserUnusedPath(@Path("id") id: String): Any

    // ⚠️ @GET con @Body (no permitido en ejecución)
    @GET("users/filter")
    suspend fun getUsersWithBody(@Body filter: Any): List<Any>

    // ⚠️ @DELETE con @Body (puede no ser soportado por todos los servidores ni Ktor si no se configura)
    @DELETE("users/{id}")
    suspend fun deleteUserWithBody(@Path("id") id: String, @Body audit: Any)

    // ⚠️ @Field sin @FormUrlEncoded
    @POST("submit")
    suspend fun submitDataWithoutForm(@Field("value") value: String): Any

    // ⚠️ @Part sin @Multipart
    @POST("upload")
    suspend fun uploadWithoutMultipart(@Part file: Any): Any

    // ⚠️ Parámetro no anotado
    @POST("comments")
    suspend fun postComment(content: String): Any

    // ⚠️ URL con doble slash (ej: base = https://api.com/, ruta = /endpoint)
    @GET("/redundant/slash")
    suspend fun endpointWithLeadingSlash(): Any

    // ⚠️ Tipo de retorno con body en HEAD
    @HEAD("status")
    suspend fun checkStatus(): Any

    // ⚠️ Path con mayúscula, parámetro con minúscula
    @GET("users/{ID}")
    suspend fun getByIdMismatch(@Path("id") id: String): Any

    // ⚠️ Usar @Url y @Path juntos
    @GET
    suspend fun conflictUrlAndPath(@Url url: String, @Path("id") id: String): Any
}
