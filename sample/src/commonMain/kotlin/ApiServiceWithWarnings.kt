package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.*

interface ApiServiceWithWarnings {
    // ⚠️ @GET con @Body (no permitido en ejecución)
    @GET("users/filter")
    suspend fun getUsersWithBody(@Body filter: Any): List<Any>

    // ⚠️ @Field sin @FormUrlEncoded
    @POST("submit")
    suspend fun submitDataWithoutForm(@Field("value") value: String): Any

    // ⚠️ @Part sin @Multipart
    @POST("upload")
    suspend fun uploadWithoutMultipart(@Part file: Any): Any

    // ⚠️ URL con doble slash (ej: base = https://api.com/, ruta = /endpoint)
    @GET("/redundant/slash")
    suspend fun endpointWithLeadingSlash(): Any
}
