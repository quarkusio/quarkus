package io.quarkus.it.hibernate.validator;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Test various Bean Validation operations running in native mode
 */
@QuarkusIntegrationTest
public class HibernateValidatorFunctionalityInGraalITCase extends HibernateValidatorFunctionalityTest {

    @Override
    protected boolean isTestsInJVM() {
        return false;
    }

}
