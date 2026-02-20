package io.github.kingg22.ktorgen.model.annotations

import io.github.kingg22.ktorgen.core.*
import io.github.kingg22.ktorgen.http.*

private val WHITESPACE_REGEX = "\\s+".toRegex()

fun String.removeWhitespace(): String = this.trim().replace(WHITESPACE_REGEX, "")

// experimental is a meta-annotation (documented) for optIn only. this doesn't process it
val ktorGenAnnotationsClass = setOf(
    KtorGen::class,
    KtorGenAnnotationPropagation::class,
    KtorGenVisibilityControl::class,
    KtorGenTopLevelFactory::class,
    KtorGenHttpClientExtFactory::class,
    KtorGenCompanionExtFactory::class,
)

const val KTORGEN_KMP_FACTORY = "KtorGenKmpFactory"

val ktorGenOthers = setOf(
    KtorGenExperimental::class,
)

val ktorGenAnnotationsFunction = setOf(
    KtorGenFunction::class,
    KtorGenAnnotationPropagation::class,
    HTTP::class,
    GET::class,
    POST::class,
    PUT::class,
    PATCH::class,
    DELETE::class,
    HEAD::class,
    OPTIONS::class,
    Header::class,
    Cookie::class,
    FormUrlEncoded::class,
    Multipart::class,
    Fragment::class,
)

val ktorGenAnnotationsParameter = setOf(
    Body::class,
    Cookie::class,
    Url::class,
    Field::class,
    FieldMap::class,
    HeaderMap::class,
    HeaderParam::class,
    Part::class,
    PartMap::class,
    Path::class,
    Query::class,
    QueryMap::class,
    QueryName::class,
    Tag::class,
)

val ktorGenAnnotations =
    ktorGenAnnotationsClass + ktorGenOthers + ktorGenAnnotationsFunction + ktorGenAnnotationsParameter
