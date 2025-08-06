package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.kingg22.ktorgen.DiagnosticTimer
import io.github.kingg22.ktorgen.model.ClassData

fun interface KtorGenGenerator {
    fun generate(classData: ClassData, timer: DiagnosticTimer.DiagnosticSender): FileSpec

    companion object {
        /** Generate the Impl class using [KotlinpoetGenerator] of ksp */
        fun generateKsp(classData: ClassData, codeGenerator: CodeGenerator, timer: DiagnosticTimer.DiagnosticSender) {
            DEFAULT.generate(classData, timer).writeTo(codeGenerator, false)
        }

        val DEFAULT: KtorGenGenerator by lazy { KotlinpoetGenerator() }

        val TODO_GENERATOR by lazy {
            KtorGenGenerator { classData, _ ->
                val interfaceName = classData.interfaceName

                val classBuilder = TypeSpec.classBuilder(classData.generatedName)
                    .addModifiers(KModifier.PUBLIC)
                    .addSuperinterface(ClassName(classData.packageNameString, interfaceName))
                    .addKdoc(classData.customHeader)

                classData.functions.forEach { func ->
                    val funBuilder = FunSpec.builder(func.name)
                        .addModifiers(func.modifierSet)
                        .returns(func.returnTypeData.typeName)
                        .addAnnotations(func.nonKtorGenAnnotations)
                        .addKdoc(func.customHeader)

                    if (func.isSuspend) funBuilder.addModifiers(KModifier.SUSPEND)

                    func.parameterDataList.forEach { param ->
                        funBuilder.addParameter(
                            param.nameString,
                            param.typeData.typeName,
                            buildList { if (param.isVararg) add(KModifier.VARARG) },
                        )
                    }

                    funBuilder.addCode("return TODO()")

                    classBuilder.addFunction(funBuilder.build())
                }

                val fileBuilder = FileSpec.builder(classData.packageNameString, classData.generatedName)
                    .addFileComment(classData.customHeader)
                    .addAnnotation(
                        AnnotationSpec.builder(Suppress::class)
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                            .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                            .addMember("%S", "unused")
                            .addMember("%S", "UNUSED_IMPORT")
                            .build(),
                    )

                classData.imports.forEach {
                    fileBuilder.addImport(it.substringBeforeLast("."), it.substringAfterLast("."))
                }

                fileBuilder.addType(classBuilder.build()).build()
            }
        }

        val NO_OP by lazy {
            KtorGenGenerator { data, _ ->
                FileSpec.builder(data.packageNameString, data.generatedName)
                    .addFileComment(
                        "This class is generated to test the KtorGen compiler. It does not contain any code and should not be used.",
                    )
                    .addProperty("hello", String::class, KModifier.PRIVATE, KModifier.CONST)
                    .build()
            }
        }
    }
}
