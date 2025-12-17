#############################################
# ktorgen – Android consumer ProGuard rules #
#############################################

# Maintain BINARY annotations used by generated code
# BINARY annotations exist only to make the bytecode valid
-keep,allowoptimization,allowshrinking,allowobfuscation @interface io.github.kingg22.ktorgen.core.Generated
-keep,allowoptimization,allowshrinking,allowobfuscation @interface io.github.kingg22.ktorgen.core.KtorGenExperimental

# These annotations have no runtime effects
-assumenosideeffects @interface io.github.kingg22.ktorgen.core.Generated
-assumenosideeffects @interface io.github.kingg22.ktorgen.core.KtorGenExperimental
