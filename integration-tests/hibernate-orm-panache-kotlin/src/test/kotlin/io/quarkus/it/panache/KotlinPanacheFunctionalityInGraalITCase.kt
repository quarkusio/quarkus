package io.quarkus.it.panache

import io.quarkus.test.junit.NativeImageTest

/**
 * Test various Panache operations running in native mode
 */
@NativeImageTest
class KotlinPanacheFunctionalityInGraalITCase : KotlinPanacheFunctionalityTest()