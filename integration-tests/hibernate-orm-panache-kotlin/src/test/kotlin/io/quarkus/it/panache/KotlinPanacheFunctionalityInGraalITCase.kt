package io.quarkus.it.panache

import io.quarkus.test.junit.NativeImageTest
import org.junit.jupiter.api.Disabled

/**
 * Test various Panache operations running in native mode
 */
@NativeImageTest
@Disabled("Fails on GraalVM 20.1")
class KotlinPanacheFunctionalityInGraalITCase : KotlinPanacheFunctionalityTest()
