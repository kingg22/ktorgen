package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_REQUEST_PACKAGE

internal const val FORM_DATA_VARIABLE = "_multiPartDataContent"
internal const val PART_DATA_LIST_VARIABLE = "_partDataList"
internal const val FORM_DATA_CONTENT_VARIABLE = "_formDataContent"
internal const val KTOR_HTTP_PACKAGE = "io.ktor.http"
internal const val KOTLIN_PACKAGE = "kotlin"
internal const val KOTLIN_COLLECTIONS_PACKAGE = "$KOTLIN_PACKAGE.collections"
internal val KTOR_PART_DATA_CLASS = ClassName("$KTOR_HTTP_PACKAGE.content", "PartData")
internal const val THIS_HEADERS = "this.%M"
internal val KTOR_CLIENT_REQUEST_HEADER_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "headers")
internal const val LITERAL_FOREACH = "%L.forEach"
internal const val LITERAL_FOREACH_SAFE_NULL_ENTRY = "%L?.forEach { entry ->"
internal const val APPEND_STRING_LITERAL = "this.append(%S, %L)"
internal const val APPEND_STRING_STRING = "this.append(%S, %P)"
internal const val LITERAL_NN_LET = "%L?.let"
internal const val LET_RESULT = ".let { _result ->"
internal const val ITERABLE_FILTER_NULL_FOREACH = "%L?.filterNotNull()?.forEach"
internal const val ENTRY_VALUE_NN_LET = "entry.value?.let { value ->"
internal const val VALUE = $$"$value"
internal const val BODY_TYPE = ".%M<%T>()"
internal const val MEMBER_LITERAL = "%M(%L)"
internal const val THIS_MEMBER_LITERAL = "this.$MEMBER_LITERAL"
internal val BODY_FUNCTION = MemberName("io.ktor.client.call", "body", true)
internal const val ENCODED_PARAMETERS_APPEND = "this.encodedParameters.append"
internal const val PARAMETERS_APPEND = "this.parameters.append"
internal val FLOW_MEMBER = MemberName("kotlinx.coroutines.flow", "flow")
internal val KTOR_URL_TAKE_FROM_FUNCTION = MemberName(KTOR_HTTP_PACKAGE, "takeFrom", true)
internal val DECODE_URL_COMPONENTS_FUNCTION = MemberName(KTOR_HTTP_PACKAGE, "decodeURLQueryComponent", true)
internal val KTOR_HTTP_METHOD = ClassName(KTOR_HTTP_PACKAGE, "HttpMethod")
internal val KTOR_ATTRIBUTE_KEY = ClassName("io.ktor.util", "AttributeKey")
internal val KTOR_REQUEST_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "request", true)
internal val KTOR_REQUEST_SET_BODY_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "setBody", true)
internal val KTOR_URL_ENCODE_PATH = MemberName(KTOR_HTTP_PACKAGE, "encodeURLPath", true)
internal val KTOR_PARAMETERS_CLASS = ClassName(KTOR_HTTP_PACKAGE, "Parameters")
internal val KTOR_REQUEST_FORM_DATA_CONTENT_CLASS =
    ClassName("$KTOR_CLIENT_REQUEST_PACKAGE.forms", "FormDataContent")
internal val KTOR_REQUEST_MULTIPART_CONTENT_CLASS =
    ClassName("$KTOR_CLIENT_REQUEST_PACKAGE.forms", "MultiPartFormDataContent")
internal val KTOR_REQUEST_FORM_DATA_FUNCTION = MemberName("$KTOR_CLIENT_REQUEST_PACKAGE.forms", "formData")
internal val KOTLIN_LIST_OF = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "listOf")
internal val KOTLIN_EMPTY_LIST = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "emptyList")
internal val KOTLIN_MAP_OF = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "mapOf")
internal val KOTLIN_TO_PAIR_FUNCTION = MemberName(KOTLIN_PACKAGE, "to")
internal val KOTLIN_EMPTY_MAP = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "emptyMap")
internal val KTOR_GMT_DATE_CLASS = ClassName("io.ktor.util.date", "GMTDate")
internal val KTOR_REQUEST_COOKIE_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "cookie", true)
internal val KTOR_CONTENT_TYPE_FUNCTION = MemberName(KTOR_HTTP_PACKAGE, "contentType", true)
internal val KTOR_CONTENT_TYPE_CLASS = ClassName(KTOR_HTTP_PACKAGE, "ContentType")
internal val KTOR_REQUEST_TAKE_FROM_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "takeFrom", true)
internal val COROUTINES_CURRENT_CONTEXT = MemberName("kotlinx.coroutines", "currentCoroutineContext")
internal val COROUTINES_CONTEXT_ENSURE_ACTIVE = MemberName("kotlinx.coroutines", "ensureActive", true)
internal val KOTLIN_EXCEPTION_CLASS = ClassName(KOTLIN_PACKAGE, "Exception")
internal val KOTLIN_PAIR_CLASS = ClassName(KOTLIN_PACKAGE, "Pair")
