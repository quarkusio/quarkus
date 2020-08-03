package codestarts.qute - example.kotlin.src.native - test.kotlin.org.acme.qute

import io.quarkus.test.junit.NativeImageTest

@NativeImageTest
class NativeHelloResourceIT : HelloResourceTest() { // Execute the same tests but in native mode.
}