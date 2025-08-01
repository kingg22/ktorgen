@file:OptIn(ExperimentalContracts::class)

package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.extractor.DeclarationParameterMapper.Companion.getArgumentValueByName
import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.ReturnType
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import kotlin.contracts.ExperimentalContracts

class ParameterMapper : DeclarationParameterMapper {
    override fun mapToModel(declaration: KSValueParameter) = ParameterData(
        name = declaration.name?.asString().orEmpty(),
        type = ReturnType(declaration.type.resolve()),
        annotations = collectParameterAnnotations(declaration),
        isHttpRequestBuilderLambda = isHttpRequestBuilderLambda(declaration),
    )

    private fun collectParameterAnnotations(declaration: KSValueParameter): List<ParameterAnnotation> {
        val annotations = mutableListOf<ParameterAnnotation>()

        declaration.getAnnotation<Path>(
            manualExtraction = {
                annotations.add(
                    ParameterAnnotation.Path(
                        it.getArgumentValueByName<String>("value") ?: declaration.name.safeString(),
                        it.getArgumentValueByName<Boolean>("encoded") ?: false,
                    ),
                )
            },
        ) {
            annotations.add(
                ParameterAnnotation.Path(
                    it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString(), it.encoded),
                ),
            )
        }

        declaration.getAnnotation<Query>(
            manualExtraction = {
                annotations.add(
                    ParameterAnnotation.Query(
                        it.getArgumentValueByName<String>(
                            "value",
                        )?.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())
                            ?: declaration.name.safeString(),
                        it.getArgumentValueByName<Boolean>("encoded") ?: false,
                    ),
                )
            },
        ) {
            annotations.add(
                ParameterAnnotation.Query(
                    it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.encoded,
                ),
            )
        }

        declaration.getAnnotation<QueryName>(
            manualExtraction = {
                annotations.add(ParameterAnnotation.QueryName(it.getArgumentValueByName<Boolean>("encoded") ?: false))
            },
        ) {
            annotations.add(ParameterAnnotation.QueryName(it.encoded))
        }

        declaration.getAnnotation<QueryMap>(
            manualExtraction = {
                annotations.add(ParameterAnnotation.QueryMap(it.getArgumentValueByName<Boolean>("encoded") ?: false))
            },
        ) {
            annotations.add(ParameterAnnotation.QueryMap(it.encoded))
        }

        declaration.getAnnotation<Field>(
            manualExtraction = {
                annotations.add(
                    ParameterAnnotation.Field(
                        it.getArgumentValueByName<String>(
                            "value",
                        )?.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())
                            ?: declaration.name.safeString(),
                        it.getArgumentValueByName<Boolean>("encoded") ?: false,
                    ),
                )
            },
        ) {
            annotations.add(
                ParameterAnnotation.Field(
                    it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.encoded,
                ),
            )
        }

        declaration.getAnnotation<FieldMap>(
            manualExtraction = {
                annotations.add(ParameterAnnotation.FieldMap(it.getArgumentValueByName<Boolean>("encoded") ?: false))
            },
        ) {
            annotations.add(ParameterAnnotation.FieldMap(it.encoded))
        }

        declaration.getAnnotation<Part>(
            manualExtraction = {
                annotations.add(
                    ParameterAnnotation.Part(
                        it.getArgumentValueByName<String>("value") ?: declaration.name.safeString(),
                        it.getArgumentValueByName<String>("encoding") ?: "binary",
                    ),
                )
            },
        ) {
            annotations.add(ParameterAnnotation.Part(it.value, it.encoding))
        }

        declaration.getAnnotation<PartMap>(
            manualExtraction = {
                annotations.add(ParameterAnnotation.PartMap(it.getArgumentValueByName<String>("encoding") ?: "binary"))
            },
        ) {
            annotations.add(ParameterAnnotation.PartMap(it.encoding))
        }

        declaration.getAnnotation<Body>(
            manualExtraction = { _ -> annotations.add(ParameterAnnotation.Body) },
        ) { _ ->
            annotations.add(ParameterAnnotation.Body)
        }

        declaration.getAnnotation<Header>(
            manualExtraction = {
                annotations.add(
                    ParameterAnnotation.Header(
                        it.getArgumentValueByName<String>("value") ?: declaration.name.safeString(),
                    ),
                )
            },
        ) {
            annotations.add(ParameterAnnotation.Header(it.value))
        }

        declaration.getAnnotation<HeaderMap>(
            manualExtraction = { _ -> annotations.add(ParameterAnnotation.HeaderMap) },
        ) { _ ->
            annotations.add(ParameterAnnotation.HeaderMap)
        }

        declaration.getAnnotation<Url>(
            manualExtraction = { _ -> annotations.add(ParameterAnnotation.Url) },
        ) { _ ->
            annotations.add(ParameterAnnotation.Url)
        }

        declaration.getAnnotation<Tag>(
            manualExtraction = {
                annotations.add(
                    ParameterAnnotation.Tag(
                        it.getArgumentValueByName<String>("value")
                            ?.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())
                            ?: declaration.name.safeString(),
                    ),
                )
            },
        ) {
            annotations.add(
                ParameterAnnotation.Tag(it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())),
            )
        }

        return annotations
    }

    private fun isHttpRequestBuilderLambda(parameter: KSValueParameter): Boolean {
        val type = parameter.type.resolve()
        val funcType = type.declaration as? KSClassDeclaration ?: return false
        if (!funcType.simpleName.asString().startsWith("Function")) return false

        val receiver = type.annotations.firstOrNull()?.annotationType?.resolve()?.declaration?.simpleName?.asString()
        return receiver == "HttpRequestBuilder"
    }

    /** Callbacks are invoked when the annotation is present, else NO OP */
    @OptIn(KspExperimental::class)
    private inline fun <reified A : Annotation> KSValueParameter.getAnnotation(
        crossinline manualExtraction: (KSAnnotation) -> Unit,
        crossinline mapFromJvmAnnotation: (A) -> Unit,
    ) {
        try {
            this.getAnnotationsByType(A::class).firstOrNull()?.let(mapFromJvmAnnotation)
        } catch (_: Exception) {
            this.annotations.firstOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
        }
    }

    /** Avoid NPE when don't have string representation */
    private fun KSName?.safeString(): String = this?.asString() ?: ""
}
