package io.quarkus.it.hibernate.validator;

import io.quarkus.test.junit.NativeImageTest;

/**
 * Test various Bean Validation operations running in native mode
 */
@NativeImageTest
public class HibernateValidatorFunctionalityInGraalITCase extends HibernateValidatorFunctionalityTest {

    @Override
    protected boolean isTestsInJVM() {
        return false;
    }

}
