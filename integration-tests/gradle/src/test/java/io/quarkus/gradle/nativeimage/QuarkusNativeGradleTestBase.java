package io.quarkus.gradle.nativeimage;

import java.util.List;

import io.quarkus.gradle.QuarkusGradleTestBase;

public class QuarkusNativeGradleTestBase extends QuarkusGradleTestBase {

    @Override
    protected List<String> arguments(String... argument) {
        // The properties below are propagated from Maven
        // but if those properties were not actually set to proper values
        // then they would be propagated as property expressions ${prop-name}.
        // This clears them in case they appear to be expressions.
        clearPropertyIfNotSet("quarkus.native.container-build");
        clearPropertyIfNotSet("quarkus.native.builder-image");
        return super.arguments(argument);
    }

    private static void clearPropertyIfNotSet(String propName) {
        final String value = System.getProperty(propName);
        if (value != null && ("${" + propName + "}").equals(value)) {
            System.clearProperty(propName);
        }
    }
}
