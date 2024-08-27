package io.quarkus.test.common;

/**
 * Defines how Quarkus behaves with regard to the application of the resource to this test and the testsuite in general
 */
public enum TestResourceScope {

    /**
     * The declaration order must be from the narrowest scope to the widest
     */
    RESTRICTED_TO_CLASS, // means that Quarkus will run the test in complete isolation, i.e. it will restart every time it finds such a resource
    MATCHING_RESOURCE, // means that Quarkus will not restart when running consecutive tests that use the same resource
    GLOBAL, // means the resource applies to all tests in the testsuite
}
