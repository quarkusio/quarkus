package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;

import com.google.common.collect.ImmutableMap;

/**
 * A test to check if a proto file that imports another proto from dependencies
 * works properly in dev mode if quarkus.generate-code.grpc.scan-for-imports=true is specified
 */
@Disabled
public class GrpcWithIncludesDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "grpc-include-project";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).isEqualTo("hello 2");

        replace("application/src/main/proto/hello.proto",
                ImmutableMap.of("TEST_ONE = 2;", "TEST_ONE = 15;"));

        Thread.sleep(1000);

        assertThat(getHttpResponse("/hello")).isEqualTo("hello 15");
    }
}
