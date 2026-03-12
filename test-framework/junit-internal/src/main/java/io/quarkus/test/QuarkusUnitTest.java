package io.quarkus.test;

/**
 * @deprecated use {@link QuarkusExtensionTest}
 */
// When we delete this, also remove the superclass and consolidate function back into QuarkusExtensionTest
@Deprecated(since = "3.35", forRemoval = true)
public class QuarkusUnitTest extends AbstractQuarkusExtensionTest<QuarkusUnitTest> {

    public QuarkusUnitTest() {
        super();
    }

    public QuarkusUnitTest(boolean useSecureConnection) {
        super(useSecureConnection);
    }

    public static QuarkusUnitTest withSecuredConnection() {
        return new QuarkusUnitTest(true);
    }
}
