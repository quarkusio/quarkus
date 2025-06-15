package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

public class JandexKordampMultiModuleProjectDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "jandex-basic-multi-module-project-kordamp";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", ":application:quarkusDev" };
    }

    @Override
    protected void testDevMode() {

        assertThat(getHttpResponse("/hello")).contains("hello kordamp jandex common-kordamp");

        replace("common/src/main/java/org/acme/common/CommonBean.java",
                ImmutableMap.of("return \"common-kordamp\";", "return \"modified-kordamp\";"));

        assertUpdatedResponseContains("/hello", "hello kordamp jandex modified-kordamp");
    }
}
