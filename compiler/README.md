# Annotation processor of KtorGen

## Rules:
** Obvious rules:
* No return type (KSP error) → fatal error
* No file associate to interface → fatal error
* No HTTP annotation → fatal error, unknow method == unknow implementation

** a little bit of opinionated rules:

* `@HEAD` no debe devolver un body → error fatal si el tipo de retorno no es Unit.
* `@PATCH` sin `@Body` → advertencia.
* Dos parámetros con la misma anotación y nombre (`@Query("id") val id1, @Query("id") val id2`) → colisión.
* Mezclar `@Body` con `@FormUrlEncoded` o `@Multipart` → conflicto de tipo de cuerpo.
* Más de un parámetro `@Body` → no permitido
* `@Field` sin `@FormUrlEncoded` → advertencia.
* `@Part` sin `@Multipart` → advertencia.
* Usar `@Body` en método que no admite cuerpo (GET, HEAD, HttpMethod...) → error.
* `@FormUrlEncoded` solo puede usarse con `@POST`, `@PUT`, `@PATCH`.
* `@Multipart` requiere al menos un `@Part` o `@PartMap`.
* `@Header` o `@HeaderMap` con claves duplicadas → colisión, error fatal.
* `@Header` con valor nulo (es decir, luego de separar name: value, no esta completo) → error fatal
* Función de tipo retorno no es `Flow` y no es suspend → error fatal, solo los Flow pueden no ser suspend
* URL con doble slash (//) tras concatenar  ruta → error de sintaxis.
* Herencia de interfaz pero con `@KtorGenIgnore` en la super → erro fatal, no se debe generar la interfaz hija, confusión.
* Herencia de interfaz, se sobre escribe función, pero la función en la interfaz super esta con `@KtorGenIgnore` → error fatal, no se debe generar la función, confusión
* Función ya implementada en la interfaz, pero no marcada con `@KtorGenIgnore` → no se debe volver a generar, advertencia falta de anotación
* Interfaz marcada con `@KtorGen` pero sin funciones HTTP dentro → advertencia.
* Parámetros sin anotación (ni `@Query`, ni `@Body`, etc.) → error fatal: ¿cómo se enviarán?
* Nombres de parámetros no coinciden con nombre en `@Path` → error fatal de sintaxis

#### Group by type
Errores Fatales:

* @HEAD no debe devolver un body → error fatal si el tipo de retorno no es Unit
* Colisión de parámetros con la misma anotación y nombre
* Mezclar @Body con @FormUrlEncoded o @Multipart
* Más de un parámetro @Body
* Usar @Body en método que no admite cuerpo (GET, HEAD, etc.)
* @Header con claves duplicadas o formato inválido
* Función no suspend y no Flow
* URL con doble slash (//) tras concatenar ruta
* Herencia de interfaz con @KtorGenIgnore en la super
* Sobrescritura de función con @KtorGenIgnore en la super
* Parámetros sin anotación HTTP
* Nombres de parámetros de path que no coinciden

Advertencias:

* @PATCH sin @Body
* @Field sin @FormUrlEncoded
* @Part sin @Multipart
* Función implementada sin @KtorGenIgnore
* Interfaz con @KtorGen pero sin funciones HTTP

Validaciones Adicionales:

* @FormUrlEncoded solo con @POST, @PUT, @PATCH
* @Multipart requiere al menos un @Part o @PartMap
