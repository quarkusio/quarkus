package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class JandexVlsiMultiModuleProjectDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "jandex-basic-multi-module-project-vlsi";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":application:quarkusDev" };
    }

    @Override
    protected void testDevMode() {

        assertThat(getHttpResponse("/hello")).contains("hello vlsi jandex common-vlsi");

        replace("common/src/main/java/org/acme/common/CommonBean.java",
                ImmutableMap.of("return \"common-vlsi\";", "return \"modified-vlsi\";"));

        assertUpdatedResponseContains("/hello", "hello vlsi jandex modified-vlsi");
    }
}
