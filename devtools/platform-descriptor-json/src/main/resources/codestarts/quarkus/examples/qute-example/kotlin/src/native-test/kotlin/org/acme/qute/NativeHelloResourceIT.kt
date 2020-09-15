package org.acme.qute

import io.quarkus.test.junit.NativeImageTest

@NativeImageTest
class NativeHelloResourceIT : HelloResourceTest() { // Execute the same tests but in native mode.
}