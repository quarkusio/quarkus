package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class HttpStaticDirConfigTest {

    @Test
    void normalizedEndpointShouldHandleRootPath() {
        HttpStaticDirConfig config = new TestConfig("/");
        assertThat(config.normalizedEndpoint()).isEqualTo("/");
    }

    @Test
    void normalizedEndpointShouldHandleEmptyString() {
        HttpStaticDirConfig config = new TestConfig("");
        assertThat(config.normalizedEndpoint()).isEqualTo("/");
    }

    @Test
    void normalizedEndpointShouldAddLeadingSlash() {
        HttpStaticDirConfig config = new TestConfig("static");
        assertThat(config.normalizedEndpoint()).isEqualTo("/static");
    }

    @Test
    void normalizedEndpointShouldRemoveTrailingSlash() {
        HttpStaticDirConfig config = new TestConfig("/static/");
        assertThat(config.normalizedEndpoint()).isEqualTo("/static");
    }

    @Test
    void normalizedEndpointShouldRemoveMultipleTrailingSlashes() {
        HttpStaticDirConfig config = new TestConfig("/static///");
        assertThat(config.normalizedEndpoint()).isEqualTo("/static");
    }

    @Test
    void normalizedEndpointShouldHandleLeadingAndTrailingSlashes() {
        HttpStaticDirConfig config = new TestConfig("/static/");
        assertThat(config.normalizedEndpoint()).isEqualTo("/static");
    }

    @Test
    void normalizedEndpointShouldTrimWhitespace() {
        HttpStaticDirConfig config = new TestConfig("  /static  ");
        assertThat(config.normalizedEndpoint()).isEqualTo("/static");
    }

    @Test
    void normalizedEndpointShouldRejectWildcard() {
        HttpStaticDirConfig config = new TestConfig("/static/*");
        assertThatThrownBy(config::normalizedEndpoint)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain '*'");
    }

    @Test
    void normalizedEndpointShouldRejectDoubleDots() {
        HttpStaticDirConfig config = new TestConfig("/static/..");
        assertThatThrownBy(config::normalizedEndpoint)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain '..'");
    }

    @Test
    void normalizedEndpointShouldRejectSpaces() {
        HttpStaticDirConfig config = new TestConfig("/static files");
        assertThatThrownBy(config::normalizedEndpoint)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain spaces");
    }

    @Test
    void normalizedEndpointShouldRejectBackslash() {
        HttpStaticDirConfig config = new TestConfig("/static\\files");
        assertThatThrownBy(config::normalizedEndpoint)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use '/' as separator");
    }

    @Test
    void normalizedEndpointShouldHandleNestedPaths() {
        HttpStaticDirConfig config = new TestConfig("/api/v1/static");
        assertThat(config.normalizedEndpoint()).isEqualTo("/api/v1/static");
    }

    private static class TestConfig implements HttpStaticDirConfig {
        private final String endpoint;

        TestConfig(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public String endpoint() {
            return endpoint;
        }

        @Override
        public Optional<String> path() {
            return Optional.of("static");
        }
    }
}
