package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;

import com.google.common.collect.ImmutableMap;

@Disabled
public class GrpcDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "grpc-multi-module-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).isEqualTo("hello 2");

        replace("application/src/main/proto/devmodetest.proto",
                ImmutableMap.of("TEST_ONE = 2;", "TEST_ONE = 15;"));

        Thread.sleep(1000);

        assertThat(getHttpResponse("/hello")).isEqualTo("hello 15");
    }
}
