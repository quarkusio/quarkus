package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.Iterators;

import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.common.AbstractConfigSource;

public class RestClientConfigNotationTest {

    private static final String URL = "localhost:8080";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoClient.class, TestConfigSource.class)
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            "io.quarkus.restclient.configuration.RestClientConfigNotationTest$TestConfigSource"));

    @ParameterizedTest
    @ValueSource(strings = {
            "quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url",
            "quarkus.rest-client.\"EchoClient\".url",
            "quarkus.rest-client.EchoClient.url",
            "io.quarkus.restclient.configuration.EchoClient/mp-rest/url"
    })
    public void testInterfaceConfiguration(final String urlPropertyName) {
        TestConfigSource.urlPropertyName = urlPropertyName;

        RestClientsConfig configRoot = new RestClientsConfig();
        RestClientConfig clientConfig = configRoot.getClientConfig(EchoClient.class);
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
        RestClientsConfig configRoot = new RestClientsConfig();
        RestClientConfig clientConfig = configRoot.getClientConfig("echo-client");

        verifyConfig(clientConfig, urlPropertyName);
    }

    private void verifyConfig(final RestClientConfig clientConfig, final String urlPropertyName) {
        ConfigSource configSource = Iterators.find(ConfigProvider.getConfig().getConfigSources().iterator(),
                c -> c.getName().equals(TestConfigSource.SOURCE_NAME));
        assertThat(configSource.getPropertyNames()).containsOnly(urlPropertyName);
        assertThat(configSource.getValue(urlPropertyName)).isEqualTo(URL);

        assertThat(clientConfig.url).isPresent();
        assertThat(clientConfig.url.get()).isEqualTo(URL);
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
