# Examples of live applications

Examples exercising frameworks like Weld, Undertow etc using Quarkus as boot bed.

## How to run them

To run from your IDE tests inheriting from `GraalTest` (e.g. `@RunWith(GraalTest.class)`), you must define the `native.image.path` to the native image build of that specific example.
For example: `-Dnative.image.path=/full/qualified/path/to/examples/example1/quarkus-test-deployment-999-SNAPSHOT`

If this is not specified the test runner will attempt to guess the image location, which may not be successful.

## List of examples

### Strict

Strict are exercising frameworks that do not require `-H:+ReportUnsupportedElementsAtRuntime`
At the time of writing Weld needs it and thus is not in the strict category.

Strict contains one JPA entity that is here for the JPA core work.
Please ignore.

This deployment uses a 'war' packaging, but still results in the same output with a
runnable jar and a native image.

### Permissive

Permissive exercise all frameworks including the ones needing `-H:+ReportUnsupportedElementsAtRuntime`

### Class transformer

Test class transformation

### Shared library

Test the ability to index shared library