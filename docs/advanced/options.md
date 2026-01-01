# Options of processor

## Error checking type

**name**: _ktorgen_check_type_

**value type**: Int

**values**:

  * 0: Disable error checking
  * 1: Raise all warnings and errors
  * 2: Convert errors into warnings (**default**)

### Example

```kotlin
ksp {
    // optional, additional configuration for KSP
    arg("ktorgen_check_type", "2")
}
```

## Print Stacktrace on Exceptions

**name**: _ktorgen_print_stacktrace_on_exception_

**value type**: Boolean (**default**: false)

### Example

```kotlin
ksp {
    // to help in debug errors,
    // but --stacktrace of Gradle can help with suppressed and caused exception
    arg("ktorgen_print_stacktrace_on_exception", "true")
}
```

_More option soon..._
