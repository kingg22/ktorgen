package io.github.kingg22.ktorgen.validator

/** Sequential validation with the same context, accumulative result, after verifying can continue or raise errors */
interface ValidatorStrategy {
    fun validate(context: ValidationContext): ValidationResult
}
