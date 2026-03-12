package io.quarkus.test;

/**
 * A test extension for testing Quarkus internals, not intended for end user consumption
 */
public class QuarkusExtensionTest extends AbstractQuarkusExtensionTest<QuarkusExtensionTest> {

    public QuarkusExtensionTest() {
        super();
    }

    public QuarkusExtensionTest(boolean useSecureConnection) {
        super(useSecureConnection);
    }

    public static QuarkusExtensionTest withSecuredConnection() {
        return new QuarkusExtensionTest(true);
    }
}
