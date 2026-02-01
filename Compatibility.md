# Compatibility configuration

> **Key context (previous summary):**
> * [**KtorGen** uses **Kotlin 2.2.20**](https://github.com/kingg22/ktorgen/blob/main/gradle/libs.versions.toml#L27)
> (aligned with [androidx](https://github.com/androidx/androidx/blob/androidx-main/gradle/libs.versions.toml#L55)).
> * The **JS/WASM** target is **strict** and sensitive to compilation incompatibilities.
> * **Ktor Client** supports **JS/WASM** from version 3.x**.
>   * Although versions < 3 might compile, **they are not recommended**
> (released in 2024–11 or earlier and outside the scope of KtorGen support).
> * If an unsupported combination works, it is **coincidence**, not a guarantee.
> * It's expected a new version of Kotlin/KSP/Ktor will be going to work fine with the latest **KtorGen** version.
> Don't be afraid to try it out!

---

### Compatibility Matrix (Kotlin / KSP / Ktor)

| Kotlin | KSP          | Ktor  | Result    |
|--------|--------------|-------|-----------|
| 2.3.0  | 2.3.5        | 3.4.0 | ✅ Success |
| 2.3.0  | 2.3.5        | 3.3.3 | ✅ Success |
| 2.3.0  | 2.3.4        | 3.3.3 | ✅ Success |
| 2.3.0  | 2.3.3        | 3.3.2 | ✅ Success |
| 2.3.0  | 2.3.2        | 3.3.1 | ✅ Success |
| 2.2.21 | 2.3.1        | 3.3.0 | ✅ Success |
| 2.2.21 | 2.2.21-2.0.4 | 3.2.3 | ✅ Success |
| 2.2.20 | 2.3.0        | 3.0.0 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.4 | 3.2.0 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.4 | 3.1.3 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.3 | 3.3.0 | ✅ Success |
| 2.2.20 | 2.2.20-2.0.2 | 3.2.2 | ✅ Success |

---

### Failed/Unsupported Cases

| Kotlin | KSP           | Ktor   | Result   | Reason                                       |
|--------|---------------|--------|----------|----------------------------------------------|
| 2.2.21 | 2.3.1         | 2.3.13 | ❌ Failed | **Ktor Client < 3 does not support JS/WASM** |
| 2.2.0  | 2.2.0-2.0.2   | 3.2.0  | ❌ Failed | Kotlin < 2.2.20 minimum required             |
| 2.1.20 | 2.1.20-1.0.32 | 3.0.0  | ❌ Failed | Kotlin < minimum required                    |
| 2.0.21 | 2.0.21-1.0.28 | 2.3.13 | ❌ Failed | Incompatible Kotlin + Ktor without WASM      |
| 1.9.24 | 1.9.24-1.0.20 | 2.3.0  | ❌ Failed | Completely unsupported                       |

---

### Source of data

Extracted from GitHub Releases with GH CLI without pre-releases:

```json
ktor:
[18]{publishedAt,tagName}:
"2025-11-27T12:13:53Z",3.3.3
"2025-11-05T14:08:06Z",3.3.2
"2025-10-10T06:19:56Z",3.3.1
"2025-09-12T11:59:39Z",3.3.0
"2025-07-29T14:18:09Z",3.2.3
"2025-07-14T11:45:21Z",3.2.2
"2025-07-04T08:57:57Z",3.2.1
"2025-06-13T12:19:28Z",3.2.0
"2025-05-06T11:52:28Z",3.1.3
"2025-03-28T09:27:52Z",3.1.2
"2025-02-25T11:49:42Z",3.1.1
"2025-02-12T11:44:49Z",3.1.0
"2024-12-19T14:25:41Z",3.0.3
"2024-12-04T09:48:12Z",3.0.2
"2024-10-30T08:46:03Z",3.0.1
"2024-10-10T09:46:01Z",3.0.0
kotlin:
[3]{publishedAt,tagName}:
"2025-12-16T13:29:31Z",v2.3.0
"2025-10-23T10:35:10Z",v2.2.21
"2025-09-10T08:33:26Z",v2.2.20
ksp:
[9]{publishedAt,tagName}:
"2025-12-16T20:35:23Z",2.3.4
"2025-11-20T20:58:40Z",2.3.3
"2025-11-06T20:22:33Z",2.3.2
"2025-11-04T17:29:37Z",2.3.1
"2025-10-28T20:11:15Z",2.2.21-2.0.4
"2025-10-22T16:26:19Z",2.3.0
"2025-10-07T21:52:47Z",2.2.20-2.0.4
"2025-09-11T21:48:29Z",2.2.20-2.0.3
"2025-09-10T21:04:57Z",2.2.20-2.0.2
```

---

### Clear Conclusions

* ✅ **Minimum safe configuration**:
  * **Kotlin ≥ 2.2.20**
    * ❌ **Kotlin < 2.2.20**:
      * Incompatible with **KtorGen**
      * Guaranteed build failures in WASM
  * **Ktor ≥ 3.0.0**
