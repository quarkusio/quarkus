package io.quarkus.gradle.nativeimage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.quarkus.gradle.BuildResult;
import io.quarkus.gradle.QuarkusGradleWrapperTestBase;

public class QuarkusNativeGradleITBase extends QuarkusGradleWrapperTestBase {

    private final static String[] NATIVE_ARGS = new String[] {
            "quarkus.native.container-build",
            "quarkus.native.builder-image"
    };

    @Override
    public BuildResult runGradleWrapper(File projectDir, String... args) throws IOException, InterruptedException {

        List<String> nativeArgs = new LinkedList<>();
        nativeArgs.addAll(Arrays.asList(args));
        // The properties below are propagated from Maven
        // but if those properties were not actually set to proper values
        // then they would be propagated as property expressions ${prop-name}.
        // This clears them in case they appear to be expressions.
        for (String propName : NATIVE_ARGS) {
            final String value = System.getProperty(propName);
            if (value != null && ("${" + propName + "}").equals(value)) {
                System.clearProperty(propName);
            } else if (value != null) {
                nativeArgs.add(String.format("-D%s=%s", propName, value));
            }
        }
        return super.runGradleWrapper(projectDir, nativeArgs.toArray(new String[0]));
    }
}
