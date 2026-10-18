package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

public class IncludedKotlinBuildDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "included-kotlin-build";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("Hello from SomeBean!");

        replace("acme-subproject-nested/acme-nested/src/main/kotlin/org/example/SomeBean.kt",
                Map.of("Hello from SomeBean!", "Bye!"));

        assertUpdatedResponseContains("/hello", "Bye!");
    }
}
