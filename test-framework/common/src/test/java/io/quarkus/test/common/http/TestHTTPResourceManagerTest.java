package io.quarkus.test.common.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.ValueRegistryImpl;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfigBuilder;

class TestHTTPResourceManagerTest {

    private static final RuntimeKey<URI> LAMBDA_BASE_URI = RuntimeKey.key("quarkus.lambda.local-base-uri");
    private static final RuntimeKey<URI> LOCAL_BASE_URI = RuntimeKey.key("quarkus.http.local-base-uri");

    @Test
    void usesLambdaLocalBaseUri() {
        ValueRegistry valueRegistry = ValueRegistryImpl.builder().build();
        valueRegistry.register(LAMBDA_BASE_URI, URI.create("http://localhost:8085/my-api"));

        String url = TestHTTPResourceManager.testUrl(valueRegistry, config(), "resource");

        assertThat(url).isEqualTo("http://localhost:8085/my-api/resource");
    }

    @Test
    void usesRegularLocalBaseUriWhenLambdaBaseUriIsAbsent() {
        ValueRegistry valueRegistry = ValueRegistryImpl.builder().build();
        valueRegistry.register(LOCAL_BASE_URI, URI.create("http://localhost:8085"));

        String url = TestHTTPResourceManager.testUrl(valueRegistry, config(), "my-api");

        assertThat(url).isEqualTo("http://localhost:8085/my-api");
    }

    private static Config config() {
        return new SmallRyeConfigBuilder().withDefaultValues(Map.of()).build();
    }
}
