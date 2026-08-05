package io.quarkus.it.vertx.nativetransport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that the build fails when {@code native-transport=required} and the requested native
 * transport dependency is not on the classpath.
 */
class NativeTransportRequiredFailureTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GreetingResource.class)
                    .addAsResource("application.properties"))
            .setApplicationName("native-transport-required-failure")
            .setApplicationVersion("0.1-SNAPSHOT")
            .assertBuildException(t -> {
                assertThat(t).hasMessageContaining("Native transport 'epoll' was requested")
                        .hasMessageContaining("dependency is not on the classpath");
            });

    @Test
    void buildShouldFailWhenRequiredTransportMissing() {
        // the build fails before the application starts — nothing to assert here
    }
}
