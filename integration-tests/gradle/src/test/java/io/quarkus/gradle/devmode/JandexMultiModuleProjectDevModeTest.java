package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class JandexMultiModuleProjectDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "jandex-basic-multi-module-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":application:quarkusDev", "-s" };
    }

    protected void testDevMode() {

        assertThat(getHttpResponse("/hello")).contains("hello common");

        replace("common/src/main/java/org/acme/common/CommonBean.java",
                ImmutableMap.of("return \"common\";", "return \"modified\";"));

        assertUpdatedResponseContains("/hello", "hello modified");
    }
}
