package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class ImplementationFilesDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "implementation-files";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":common:build", ":application-dep:quarkusDev", "--no-build-cache" };
    }

    @Override
    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse("/hello")).contains("hello common");

        replace("application-dep/src/main/java/org/acme/quarkus/sample/HelloResource.java",
                ImmutableMap.of("return \"hello \" + common.getName();", "return \"hi \" + common.getName();"));

        assertUpdatedResponseContains("/hello", "hi common");
    }
}
