package io.quarkus.test.timeouts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TimeoutResolverTest {

    private static final String FILE = """
            timeout=20s
            extensions.timeout=40s
            extensions/redis-client/runtime.timeout=30s
            extensions/redis-client/runtime.PubSubTest.timeout=50s
            integration-tests/grpc-hibernate.BlockingMutinyTest.shouldAdd.timeout=1m
            """;

    @AfterEach
    void clear() {
        System.clearProperty(TimeoutResolver.DEFAULT_PROPERTY);
        System.clearProperty(TimeoutResolver.MULTIPLIER_PROPERTY);
    }

    private static TimeoutResolver resolver(String content) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        return new TimeoutResolver(properties);
    }

    @Test
    void mostSpecificWins() throws Exception {
        TimeoutResolver resolver = resolver(FILE);
        assertThat(resolver.resolve("integration-tests/grpc-hibernate", "BlockingMutinyTest", "shouldAdd"))
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(resolver.resolve("extensions/redis-client/runtime", "PubSubTest", "other"))
                .isEqualTo(Duration.ofSeconds(50));
        assertThat(resolver.resolve("extensions/redis-client/runtime", "StringCommandsTest", null))
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(resolver.resolve("extensions/redis-client/deployment", "StringCommandsTest", null))
                .isEqualTo(Duration.ofSeconds(40));
        assertThat(resolver.resolve("integration-tests/grpc-hibernate", "BlockingMutinyTest", "other"))
                .isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void unknownModuleUsesGlobal() throws Exception {
        assertThat(resolver(FILE).resolve(null, "Foo", "bar")).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void fallbackWhenNothingConfigured() throws Exception {
        assertThat(resolver("").resolve("extensions/foo", "Foo", "bar")).isEqualTo(TimeoutResolver.FALLBACK);
    }

    @Test
    void systemPropertyReplacesGlobalOnly() throws Exception {
        System.setProperty(TimeoutResolver.DEFAULT_PROPERTY, "7s");
        TimeoutResolver resolver = resolver(FILE);
        assertThat(resolver.resolve("integration-tests/x", "Foo", "bar")).isEqualTo(Duration.ofSeconds(7));
        assertThat(resolver.resolve("extensions/x", "Foo", "bar")).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    void multiplierScalesResult() throws Exception {
        System.setProperty(TimeoutResolver.MULTIPLIER_PROPERTY, "3");
        assertThat(resolver(FILE).resolve("extensions/x", "Foo", "bar")).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void parsesUnits() {
        assertThat(TimeoutResolver.parse("500ms")).isEqualTo(Duration.ofMillis(500));
        assertThat(TimeoutResolver.parse("2m")).isEqualTo(Duration.ofMinutes(2));
        assertThat(TimeoutResolver.parse("PT5S")).isEqualTo(Duration.ofSeconds(5));
        assertThatThrownBy(() -> TimeoutResolver.parse("soon")).isInstanceOf(IllegalArgumentException.class);
    }
}
