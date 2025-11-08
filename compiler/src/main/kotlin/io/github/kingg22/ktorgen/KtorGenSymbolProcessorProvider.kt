package io.github.kingg22.ktorgen

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KtorGenSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val ktorGenOptions = KtorGenOptions(environment.options)
        return KtorGenProcessor(environment, KtorGenLogger(environment.logger, ktorGenOptions), ktorGenOptions)
    }
}
