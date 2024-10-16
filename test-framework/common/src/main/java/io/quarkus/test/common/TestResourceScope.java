package io.quarkus.test.common;

/**
 * Defines how Quarkus behaves with regard to the application of the resource to this test and the testsuite in general
 */
public enum TestResourceScope {

    /*
     * The declaration order must be from the narrowest scope to the widest
     */

    /**
     * Means that Quarkus will run the test in complete isolation, i.e. it will restart every time it finds such a resource.
     * <p>
     * Restarting Quarkus for this test means that the test resources will be restarted.
     * This includes the global test resources and the Dev Services.
     * <p>
     * Use with caution as it might slow down your test suite significantly.
     */
    RESTRICTED_TO_CLASS,

    /**
     * Means that Quarkus will not restart when running consecutive tests that use the same set of resources.
     * <p>
     * Note that when a restart is needed, all the resources will be restarted.
     * This includes the global test resources and the Dev Services.
     * <p>
     * Quarkus groups the tests by test resources to reduce the number of restarts.
     */
    MATCHING_RESOURCES,

    /**
     * Means the resource applies to all tests in the test suite.
     */
    GLOBAL
}
