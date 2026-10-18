package io.quarkus.amazon.lambda.http.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

class AmazonLambdaHttpProcessorTest {

    @Test
    void devModeShouldReturnMarkerToAllowRealHttpSocket() {
        AmazonLambdaHttpProcessor processor = new AmazonLambdaHttpProcessor();
        LaunchModeBuildItem devMode = new LaunchModeBuildItem(
                LaunchMode.DEVELOPMENT, Optional.empty(), false, Optional.empty(), false);

        RequireVirtualHttpBuildItem result = processor.requestVirtualHttp(devMode);

        assertThat(result).isSameAs(RequireVirtualHttpBuildItem.MARKER);
        assertThat(result.isAlwaysVirtual()).isFalse();
    }

    @Test
    void testModeShouldReturnAlwaysVirtual() {
        AmazonLambdaHttpProcessor processor = new AmazonLambdaHttpProcessor();
        LaunchModeBuildItem testMode = new LaunchModeBuildItem(
                LaunchMode.TEST, Optional.empty(), false, Optional.empty(), true);

        RequireVirtualHttpBuildItem result = processor.requestVirtualHttp(testMode);

        assertThat(result).isSameAs(RequireVirtualHttpBuildItem.ALWAYS_VIRTUAL);
        assertThat(result.isAlwaysVirtual()).isTrue();
    }

    @Test
    void productionModeShouldReturnAlwaysVirtual() {
        AmazonLambdaHttpProcessor processor = new AmazonLambdaHttpProcessor();
        LaunchModeBuildItem prodMode = new LaunchModeBuildItem(
                LaunchMode.NORMAL, Optional.empty(), false, Optional.empty(), false);

        RequireVirtualHttpBuildItem result = processor.requestVirtualHttp(prodMode);

        assertThat(result).isSameAs(RequireVirtualHttpBuildItem.ALWAYS_VIRTUAL);
        assertThat(result.isAlwaysVirtual()).isTrue();
    }
}
