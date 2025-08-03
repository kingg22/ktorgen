package io.github.kingg22.ktorgen.sample

import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.sample.model.IssueData
import io.ktor.http.content.PartData

interface ApiServiceWithWarnings {
    // ⚠️ @GET con @Body (no permitido en ejecución)
    @GET("users/filter")
    suspend fun getUsersWithBody(@Body filter: IssueData): List<Any>

    // ⚠️ @Field sin @FormUrlEncoded
    @POST("submit")
    suspend fun submitDataWithoutForm(@Field("value") vararg value: String): IssueData

    // ⚠️ @Part sin @Multipart
    @POST("upload")
    suspend fun uploadWithoutMultipart(@Part file: PartData): IssueData

    // ⚠️ URL con doble slash (ej: base = https://api.com/, ruta = /endpoint)
    @GET("/redundant/slash")
    suspend fun endpointWithLeadingSlash(): IssueData

    companion object
}
