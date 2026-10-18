package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a 'test profile'. Tests run under a test profile
 * will have different configuration options to other tests.
 *
 * Due to the global nature of Quarkus if a previous test was
 * run under a different profile then Quarkus will need to be
 * restarted when the profile changes. Unfortunately there
 * is currently no way to order tests based on profile, however
 * this can be done manually by running tests in alphabetical
 * order and putting all tests with the same profile in the same
 * package.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestProfile {

    /**
     * The test profile to use. If subsequent tests use the same
     * profile then Quarkus will not be restarted between tests,
     * giving a faster execution.
     */
    Class<? extends QuarkusTestProfile> value();

}
