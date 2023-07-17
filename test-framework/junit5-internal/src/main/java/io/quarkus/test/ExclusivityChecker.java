package io.quarkus.test;

import org.junit.jupiter.api.extension.ExtensionContext;

public class ExclusivityChecker {
    public static final String IO_QUARKUS_TESTING_TYPE = "io.quarkus.testing.type";

    public static void checkTestType(ExtensionContext extensionContext, Class<?> current) {
        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);

        Class<?> testType = store.get(IO_QUARKUS_TESTING_TYPE, Class.class);
        if (testType != null) {
            if (testType != QuarkusUnitTest.class && testType != QuarkusDevModeTest.class
                    && testType != QuarkusProdModeTest.class) {
                throw new IllegalStateException(
                        "Cannot mix both " + current.getName() + " based tests and " + testType.getName()
                                + " based tests in the same run");
            }
        } else {
            store.put(IO_QUARKUS_TESTING_TYPE, current);
        }
    }
}
