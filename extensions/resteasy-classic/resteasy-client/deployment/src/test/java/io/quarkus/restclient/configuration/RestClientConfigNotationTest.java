package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.common.AbstractConfigSource;

public class RestClientConfigNotationTest {

    private static final String URL = "localhost:8080";

    @ParameterizedTest
    @ValueSource(strings = {
            "quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url",
            "quarkus.rest-client.\"EchoClient\".url",
            "quarkus.rest-client.EchoClient.url",
            "io.quarkus.restclient.configuration.EchoClient/mp-rest/url"
    })
    public void testInterfaceConfiguration(final String urlPropertyName) {
        TestConfigSource.urlPropertyName = urlPropertyName;
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withSources(new TestConfigSource())
                .build();

        RestClientsConfig configRoot = config.getConfigMapping(RestClientsConfig.class);
        RestClientsConfig.RestClientConfig clientConfig = configRoot.getClient(EchoClient.class);
        verifyConfig(clientConfig, urlPropertyName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "quarkus.rest-client.\"echo-client\".url",
            "quarkus.rest-client.echo-client.url",
            "echo-client/mp-rest/url"
    })
    public void testConfigKeyConfiguration(final String urlPropertyName) {
        TestConfigSource.urlPropertyName = urlPropertyName;
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withSources(new TestConfigSource())
                .build();

        RestClientsConfig configRoot = config.getConfigMapping(RestClientsConfig.class);
        RestClientsConfig.RestClientConfig clientConfig = configRoot.getClient("echo-client");
        verifyConfig(clientConfig, urlPropertyName);
    }

    private void verifyConfig(final RestClientsConfig.RestClientConfig clientConfig, final String urlPropertyName) {
        ConfigSource configSource = new TestConfigSource();
        assertThat(configSource.getPropertyNames()).containsOnly(urlPropertyName);
        assertThat(configSource.getValue(urlPropertyName)).isEqualTo(URL);

        assertThat(clientConfig.url()).isPresent();
        assertThat(clientConfig.url().get()).isEqualTo(URL);
    }

    public static class TestConfigSource extends AbstractConfigSource {

        public static final String SOURCE_NAME = "Test config source";

        public static String urlPropertyName;

        public TestConfigSource() {
            super(SOURCE_NAME, Integer.MAX_VALUE);
        }

        @Override
        public Map<String, String> getProperties() {
            if (urlPropertyName != null) {
                return Collections.singletonMap(urlPropertyName, URL);
            }
            return Collections.emptyMap();
        }

        @Override
        public Set<String> getPropertyNames() {
            if (urlPropertyName != null) {
                return Set.of(urlPropertyName);
            }
            return Collections.emptySet();
        }

        @Override
        public String getValue(String propertyName) {
            if (propertyName.equals(urlPropertyName)) {
                return URL;
            }
            return null;
        }
    }
}
