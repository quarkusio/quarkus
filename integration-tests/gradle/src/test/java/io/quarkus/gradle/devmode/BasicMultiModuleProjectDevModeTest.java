package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class BasicMultiModuleProjectDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "basic-multi-module-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":application:quarkusDev", "-s" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse())
                .contains("ready")
                .contains("my-quarkus-project")
                .contains("org.acme.quarkus.sample")
                .contains("1.0-SNAPSHOT");

        assertThat(getHttpResponse("/hello")).contains("hello common");

        replace("common/src/main/java/org/acme/common/CommonBean.java",
                ImmutableMap.of("return \"common\";", "return \"modified\";"));

        assertUpdatedResponseContains("/hello", "hello modified");
    }
}
