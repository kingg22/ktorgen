package io.github.kingg22.ktorgen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KtorGenLogger(private val kspLogger: KSPLogger, private val loggingType: Int) : KSPLogger by kspLogger {
    override fun error(message: String, symbol: KSNode?) {
        when (loggingType) {
            0 -> {
                // Do nothing
            }

            1 -> {
                kspLogger.error("$KTOR_GEN $message", symbol)
            }

            2 -> {
                // Turn errors into compile warnings
                kspLogger.warn("$KTOR_GEN $message", symbol)
            }
        }
    }

    companion object {
        const val KTOR_GEN = "[KtorGen]:"
        const val KTOR_GEN_TYPE_NOT_ALLOWED =
            "$KTOR_GEN Only interfaces and it companion objects can be annotated with @KtorGen"
        const val ONLY_ONE_HTTP_METHOD_IS_ALLOWED = "$KTOR_GEN Only one HTTP method is allowed."
        const val INTERFACE_NOT_HAVE_FILE = "$KTOR_GEN Interface must be in a file, but was null: "
        const val FUNCTION_NOT_RETURN_TYPE = "$KTOR_GEN Function don't have return type: "
        const val COOKIE_ON_FUNCTION_WITHOUT_VALUE =
            "$KTOR_GEN @Cookie on function requires value, only on parameter is not needed."
        const val NO_HTTP_ANNOTATION =
            "No Http annotation found and don't have a valid HttpRequest.takeFrom() or HttpRequestBuilder lambda. Add an @HTTP or @GET or parameter with valid type for dynamic request."
        const val HEADER_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING =
            "@HeaderMap parameter type must be Map<String, String(?)> or Pair<String, String(?)>. "
        const val INVALID_HEADER = "The header name or value is blank on @Header or @HeaderParam."
        const val DUPLICATE_HEADER =
            "@Header on function + @HeaderParam on parameter(s) have duplicate values. Please unique header name is required for 'Content-Type'. "
        const val FUNCTION_OR_PARAMETERS_TYPES_MUST_NOT_INCLUDE_TYPE_VARIABLE_OR_WILDCARD =
            "Function or parameters types must not include a type variable or wildcard: "
        const val SUSPEND_FUNCTION_OR_FLOW =
            "The function should be 'suspend' or return a Flow/StateFlow/SharedFlow, but it returns "
        const val ABSTRACT_FUNCTION_IGNORED = "An abstract function with @KtorGenFunction(generate=false) is invalid. "
        const val FORM_ENCODED_MUST_CONTAIN_AT_LEAST_ONE_FIELD =
            "@FormUrlEncoded must contain at least one @Field or @FieldMap. "
        const val FORM_ENCODED_ANNOTATION_MISSING_FOUND_FIELD =
            "@FormUrlEncoded annotation missing, but found at least one @Field or @FieldMap parameter. "
        const val FORM_ENCODED_ANNOTATION_MISMATCH_HTTP_METHOD =
            "@FormUrlEncoded as body is accepted only in HTTP methods @POST, @PUT, @PATCH. "
        const val MULTIPART_ANNOTATION_MISSING_FOUND_PART =
            "@Multipart annotation missing, but found at least one @Part or @PartMap parameter. "
        const val MULTIPART_MUST_CONTAIN_AT_LEAST_ONE_PART =
            "@Multipart method must contain at least one @Part or @PartMap. "
        const val BODY_USAGE_INVALID_HTTP_METHOD =
            "A body usage in request with invalid HTTP method, Ktor must be fail. See more detail in https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.1Declaration and/or https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods "
        const val INVALID_BODY_PARAMETER = "Only one @Body parameter annotation is allowed. "
        const val CONFLICT_BODY_TYPE = "Only one type of body is allowed, @Body or @FormUrlEncoded or @Multipart. "
        const val HTTP_METHOD_HEAD_NOT_RETURN_BODY =
            "@HEAD HTTP method don't return body, only Unit as return type is accepted. See https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods/HEAD "
        const val URL_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE = "Url parameter type may not be nullable. "
        const val PATH_PARAMETER_TYPE_MAY_NOT_BE_NULLABLE = "Path parameter type may not be nullable. "
        const val PATH_CAN_ONLY_BE_USED_WITH_RELATIVE_URL_ON = "@Path can only be used with placeholder in URL"
        const val DUPLICATE_PATH_PLACEHOLDER = "@Path placeholder may be unique, found: "
        const val ONLY_ONE_CONTENT_TYPE_IS_ALLOWED = "Only one Content-Type header (or inferred by body) is allowed. "
        const val MISSING_PATH_VALUE = "Missing path value, found in path placeholders: "
        const val MULTIPLE_URL_FOUND = "Multiple @Url annotations found, only one is accepted. "
        const val URL_WITH_PATH_VALUE = "@Url parameter can only be used with empty path in Http Method. "
        const val URL_WITH_PATH_PARAMETER = "@Url parameter can't be used with @Path parameters. "
        const val PARAMETER_WITHOUT_ANNOTATION =
            "Parameter without annotation of usage is invalid. Please, indicate with annotation the reason or use HttpRequestBuilder. "
        const val PARAMETER_WITH_LOT_ANNOTATIONS =
            "Parameter with more than one annotation is invalid. Please, for advance request use HttpRequestBuilder or don't mix annotations on parameter."
        const val VARARG_PARAMETER_WITH_LOT_ANNOTATIONS =
            "vararg parameter with more than one repeatable annotation can be invalid. Use Map or ignore if you are sure."
        const val ONLY_ONE_HTTP_REQUEST_BUILDER =
            "Only one Http Request Builder is allowed per function. Found: "
        const val FIELD_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING =
            "@FieldMap parameter type must be Map<String, *(?)> or Pair<String, *(?)>. "
        const val PART_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING =
            "@PartMap parameter type must be Map<String, *(?)> or Pair<String, *(?)>. "
        const val QUERY_MAP_PARAMETER_TYPE_MUST_BE_MAP_PAIR_STRING =
            "@QueryMap parameter type must be Map<String, String> or Pair<String, String>. "
        const val VARARG_PARAMETER_EXPERIMENTAL =
            "vararg parameter is an experimental feature, currently only @HeaderMap, @HeaderParam, @Cookie support it."
        const val CONTENT_TYPE_BODY_UNKNOWN = "Content-Type for @Body maybe is unknown. "
        const val ANY_TYPE_INVALID = "'Any' type is not valid, break validation and serialization."
        const val PRIVATE_INTERFACE_CANT_GENERATE =
            "Private Interface KSP can create a valid class to implement it. Make public or internal. "
        const val ONLY_PUBLIC_INTERNAL_CLASS = "Only public or internal visibility modifier for class is valid. "
        const val MISSING_COMPANION_TO_GENERATE =
            "Missing declare explicit the companion to generate an extension function or remove @KtorGen(generateCompanionExtFunction = false). This limitation is of KSP and how kotlin generate the companion class. See KSP limitations: https://kotlinlang.org/docs/ksp-why-ksp.html#limitations and Kotlin Companion objects https://kotlinlang.org/docs/object-declarations.html#companion-objects"
        const val TWO_KTORGEN_ANNOTATIONS = "2 annotation of @KtorGen is not valid, please remove of them."
    }
}
