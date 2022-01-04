package io.quarkus.it.panache

import io.quarkus.test.junit.QuarkusIntegrationTest
import org.junit.jupiter.api.Disabled

/**
 * Test various Panache operations running in native mode
 */
@QuarkusIntegrationTest
class KotlinPanacheFunctionalityInGraalITCase : KotlinPanacheFunctionalityTest()
