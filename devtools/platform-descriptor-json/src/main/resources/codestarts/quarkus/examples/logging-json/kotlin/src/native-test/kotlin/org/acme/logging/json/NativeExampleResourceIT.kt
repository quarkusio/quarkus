package org.acme.logging.json

import io.quarkus.test.junit.NativeImageTest

@NativeImageTest
class NativeExampleResourceIT : ExampleResourceTest() { // Execute the same tests but in native mode.
}