---
hide:
  - navigation
---
### ❗ Unresolved References

If you encounter errors like:

`Found unresolved symbols on finish round: [MyApi => [kotlin.jvm.JvmOverloads]]`

> Can be a Kotlin unresolved reference, but [KSP hides that error](https://github.com/google/ksp/issues/2668) with unresolved symbols.
> Can be a misconfiguration issue, see [Configuration](configuration.md#unresolved-references)

> 🧠 **Tip:**
> `@JvmOverloads` and similar JVM annotations are only supported in `commonMain`, `jvmMain`, or `androidMain`.
> If you apply them in multiplatform configurations per source target, non-JVM targets will fail to compile due to missing expect/actual declarations.

### ⚠️ Amper support

> Current is not possible, work in progress*
