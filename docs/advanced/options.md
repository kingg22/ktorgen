# Options of processor

## Strict checking type

**name**: _ktorgen.strict_

**value type**: Int

**values**:

  * 0: Disable error checking
  * 1: Raise all warnings and errors
  * 2: Convert errors into warnings (**default**)

### Example

```kotlin
ksp {
    // optional, additional configuration for KSP
    arg("ktorgen.strict", "2")
}
```

## Print Stacktrace on Exceptions

**name**: _ktorgen.print_stacktrace_

**value type**: Boolean (**default**: false)

### Example

```kotlin
ksp {
    // to help in debug errors,
    // but --stacktrace of Gradle can help with suppressed and caused exception
    arg("ktorgen_print_stacktrace_on_exception", "true")
}
```

## Experimental features

**name**: _ktorgen.experimental_

**value type**: Boolean (**default**: false)

### Example

```kotlin
ksp {
    arg("ktorgen.experimental", "true")
}
```

_More option soon..._
