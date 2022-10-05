package io.quarkus.it.jpa.h2;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Functionality test which requires the H2 datbase to be embedded
 * within the binary produced by GraalVM native-image.
 */
@QuarkusIntegrationTest
public class JPAFunctionalityInGraalITCase extends JPAFunctionalityTest {

}
