# Java library example

This module contains an example of a Java library

THIS IS JUST AN EXAMPLE, is not recommended, but KSP works with java code.
KSP needs to be applied with kotlin gradle plugin to work.

The generated code is ALWAYS going to be in kotlin because KtorGen is for Ktor-Client,
so, it's kotlin only and needs `suspend function` to build the request.

KtorGen doesn't go to generate code for java, not generated compatibility code like `runBlocking` or `completableFuture`.

If you want to use this library in java, use retrofit or something similar.

If you have a mixed project, probably you want to use Ktor-Client for the kotlin part and add compatibility code for the java part.
Or focus on the kotlin part and use KtorGen only for the kotlin part. Let java for other parts.

### Internal testing purposes

This module tests that KtorGen can read Java code and generate compliant Kotlin code.
We can't add tests to the main project because the testing framework (Room compiler testing) isn't compatible with mixed Java–Kotlin projects.

**THIS IS NOT A REAL USE CASE, DON'T RECOMMEND USING THIS.**

**Please DON'T DO THIS.**
