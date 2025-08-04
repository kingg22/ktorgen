package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import io.github.kingg22.ktorgen.Timer
import io.github.kingg22.ktorgen.extractor.DeclarationParameterMapper.Companion.getArgumentValueByName
import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.TypeData
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ParameterMapper : DeclarationParameterMapper {
    override fun mapToModel(declaration: KSValueParameter): ParameterData {
        val timer = Timer("KtorGen [Parameter Mapper] for ${declaration.name?.asString() ?: "unknow"}").start()
        try {
            val type = declaration.type.resolve()
            return ParameterData(
                nameString = declaration.name?.asString().orEmpty(),
                typeData = TypeData(type),
                ksValueParameter = declaration,
                ktorgenAnnotations = collectParameterAnnotations(declaration).also {
                    timer.markStepCompleted("Processed annotations")
                },
                isHttpRequestBuilderLambda = isHttpRequestBuilderLambda(type).also {
                    timer.markStepCompleted("Processed is http builder lambda: $it")
                },
            ).also {
                timer.markStepCompleted("End for ${it.nameString}")
            }
        } catch (e: Throwable) {
            timer.markStepCompleted("Error on parameter")
            throw e
        } finally {
            timer.finishAndPrint()
        }
    }

    private fun collectParameterAnnotations(declaration: KSValueParameter): List<ParameterAnnotation> = buildList {
        declaration.getAnnotation<Path>(
            manualExtraction = {
                add(
                    ParameterAnnotation.Path(
                        it.getArgumentValueByName<String>("value")
                            ?.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())
                            ?: declaration.name.safeString(),
                        it.getArgumentValueByName<Boolean>("encoded") ?: false,
                    ),
                )
            },
        ) {
            add(
                ParameterAnnotation.Path(
                    it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.encoded,
                ),
            )
        }

        declaration.getAnnotation<Query>(
            manualExtraction = {
                add(
                    ParameterAnnotation.Query(
                        it.getArgumentValueByName<String>("value")
                            ?.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())
                            ?: declaration.name.safeString(),
                        it.getArgumentValueByName<Boolean>("encoded") ?: false,
                    ),
                )
            },
        ) {
            add(
                ParameterAnnotation.Query(
                    it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.encoded,
                ),
            )
        }

        declaration.getAnnotation<QueryName>(
            manualExtraction = {
                add(ParameterAnnotation.QueryName(it.getArgumentValueByName<Boolean>("encoded") ?: false))
            },
        ) { add(ParameterAnnotation.QueryName(it.encoded)) }

        declaration.getAnnotation<QueryMap>(
            manualExtraction = {
                add(ParameterAnnotation.QueryMap(it.getArgumentValueByName<Boolean>("encoded") ?: false))
            },
        ) { add(ParameterAnnotation.QueryMap(it.encoded)) }

        declaration.getAnnotation<Field>(
            manualExtraction = {
                add(
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
            add(
                ParameterAnnotation.Field(
                    it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.encoded,
                ),
            )
        }

        declaration.getAnnotation<FieldMap>(
            manualExtraction = {
                add(ParameterAnnotation.FieldMap(it.getArgumentValueByName<Boolean>("encoded") ?: false))
            },
        ) { add(ParameterAnnotation.FieldMap(it.encoded)) }

        declaration.getAnnotation<Part>(
            manualExtraction = {
                add(
                    ParameterAnnotation.Part(
                        it.getArgumentValueByName<String>("value") ?: declaration.name.safeString(),
                        it.getArgumentValueByName<String>("encoding") ?: "binary",
                    ),
                )
            },
        ) { add(ParameterAnnotation.Part(it.value, it.encoding)) }

        declaration.getAnnotation<PartMap>(
            manualExtraction = {
                add(ParameterAnnotation.PartMap(it.getArgumentValueByName<String>("encoding") ?: "binary"))
            },
        ) { add(ParameterAnnotation.PartMap(it.encoding)) }

        declaration.getAnnotation<Body>(
            manualExtraction = { _ -> add(ParameterAnnotation.Body) },
        ) { _ -> add(ParameterAnnotation.Body) }

        declaration.getAnnotation<HeaderParam>(
            manualExtraction = {
                add(
                    ParameterAnnotation.Header(
                        it.getArgumentValueByName<String>("name") ?: declaration.name.safeString(),
                    ),
                )
            },
        ) { add(ParameterAnnotation.Header(it.name)) }

        declaration.getAnnotation<HeaderMap>(
            manualExtraction = { _ -> add(ParameterAnnotation.HeaderMap) },
        ) { _ -> add(ParameterAnnotation.HeaderMap) }

        declaration.getAnnotation<Url>(
            manualExtraction = { _ -> add(ParameterAnnotation.Url) },
        ) { _ -> add(ParameterAnnotation.Url) }

        declaration.getAnnotation<Tag>(
            manualExtraction = {
                add(
                    ParameterAnnotation.Tag(
                        it.getArgumentValueByName<String>("value")
                            ?.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString())
                            ?: declaration.name.safeString(),
                    ),
                )
            },
        ) { add(ParameterAnnotation.Tag(it.value.replace(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()))) }
    }

    private fun isHttpRequestBuilderLambda(type: KSType): Boolean {
        val decl = type.declaration
        if (decl.qualifiedName?.asString() != "kotlin.Function1") return false

        val args = type.arguments
        if (args.size != 2) return false

        val receiverType = args[0].type?.resolve()?.declaration?.qualifiedName?.asString()
        val returnType = args[1].type?.resolve()?.declaration?.qualifiedName?.asString()

        return receiverType == "io.ktor.client.request.HttpRequestBuilder" &&
            returnType == "kotlin.Unit"
    }

    /** Callbacks are invoked when the annotation is present, else NO OP */
    @OptIn(KspExperimental::class, ExperimentalContracts::class)
    private inline fun <reified A : Annotation> KSValueParameter.getAnnotation(
        crossinline manualExtraction: (KSAnnotation) -> Unit,
        crossinline mapFromAnnotation: (A) -> Unit,
    ) {
        contract {
            callsInPlace(manualExtraction, InvocationKind.AT_MOST_ONCE)
            callsInPlace(mapFromAnnotation, InvocationKind.AT_MOST_ONCE)
        }
        try {
            this.getAnnotationsByType(A::class).firstOrNull()?.let(mapFromAnnotation)
        } catch (_: Exception) {
            this.annotations.firstOrNull { it.shortName.getShortName() == A::class.simpleName!! }?.let(manualExtraction)
        }
    }

    /** Avoid NPE when don't have string representation */
    private fun KSName?.safeString(): String = this?.asString() ?: ""
}
