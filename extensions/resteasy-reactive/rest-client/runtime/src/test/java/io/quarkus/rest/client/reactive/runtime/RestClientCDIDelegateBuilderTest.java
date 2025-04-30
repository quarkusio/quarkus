package io.quarkus.rest.client.reactive.runtime;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.microprofile.rest.client.ext.QueryParamStyle.COMMA_SEPARATED;
import static org.eclipse.microprofile.rest.client.ext.QueryParamStyle.MULTI_PAIRS;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CONNECTION_POOL_SIZE;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CONNECTION_TTL;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.DISABLE_CONTEXTUAL_ERROR_MESSAGES;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.KEEP_ALIVE_ENABLED;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.MAX_CHUNK_SIZE;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.MAX_REDIRECTS;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.MULTIPART_ENCODER_MODE;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.STATIC_HEADERS;
import static org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder.EncoderMode.HTML5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.restclient.config.AbstractRestClientConfigBuilder;
import io.quarkus.restclient.config.RegisteredRestClient;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

@SuppressWarnings({ "SameParameterValue" })
class RestClientCDIDelegateBuilderTest {

    private static final String TRUSTSTORE_PASSWORD = "truststorePassword";
    private static final String KEYSTORE_PASSWORD = "keystorePassword";

    private static Path truststorePath;
    private static Path keystorePath;

    @BeforeAll
    public static void beforeAll() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        // prepare keystore and truststore

        truststorePath = Files.createTempFile("truststore", ".jks");

        try (OutputStream truststoreOs = Files.newOutputStream(truststorePath)) {
            KeyStore truststore = KeyStore.getInstance("JKS");
            truststore.load(null, TRUSTSTORE_PASSWORD.toCharArray());
            truststore.store(truststoreOs, TRUSTSTORE_PASSWORD.toCharArray());
        }

        keystorePath = Files.createTempFile("keystore", ".jks");

        try (OutputStream keystoreOs = Files.newOutputStream(keystorePath)) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null, KEYSTORE_PASSWORD.toCharArray());
            keystore.store(keystoreOs, KEYSTORE_PASSWORD.toCharArray());
        }
    }

    @AfterAll
    public static void afterAll() {
        if (truststorePath != null) {
            try {
                Files.deleteIfExists(truststorePath);
            } catch (IOException e) {
                // ignore it
            }
        }
        if (keystorePath != null) {
            try {
                Files.deleteIfExists(keystorePath);
            } catch (IOException e) {
                // ignore it
            }
        }
    }

    @Test
    void clientSpecificConfigs() {
        RestClientsConfig configRoot = ConfigUtils.emptyConfigBuilder()
                .setAddDefaultSources(false)
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(new RegisteredRestClient(TestClient.class, "test-client"));
                            }
                        }.configBuilder(builder);
                    }
                })
                .withDefaultValues(createSampleConfigRoot())
                .withDefaultValues(createSampleClientConfig("test-client"))
                .build()
                .getConfigMapping(RestClientsConfig.class);

        assertEquals(1, configRoot.clients().size());
        assertTrue(configRoot.clients().containsKey(TestClient.class.getName()));

        QuarkusRestClientBuilderImpl restClientBuilderMock = Mockito.mock(QuarkusRestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).configureBuilder(restClientBuilderMock);

        verify(restClientBuilderMock).baseUri(URI.create("http://localhost:8080"));
        verify(restClientBuilderMock).property(QuarkusRestClientProperties.SHARED, true);
        verify(restClientBuilderMock).property(QuarkusRestClientProperties.NAME, "my-client");
        verify(restClientBuilderMock).property(MULTIPART_ENCODER_MODE, HTML5);

        verify(restClientBuilderMock).proxyAddress("host1", 123);
        verify(restClientBuilderMock).proxyUser("proxyUser1");
        verify(restClientBuilderMock).proxyPassword("proxyPassword1");
        verify(restClientBuilderMock).nonProxyHosts("nonProxyHosts1");
        verify(restClientBuilderMock).connectTimeout(100, MILLISECONDS);
        verify(restClientBuilderMock).readTimeout(101, MILLISECONDS);
        verify(restClientBuilderMock).userAgent("agent1");
        verify(restClientBuilderMock).property(STATIC_HEADERS, Map.of("header1", "value"));
        verify(restClientBuilderMock).property(CONNECTION_TTL, 10); // value converted to seconds
        verify(restClientBuilderMock).property(CONNECTION_POOL_SIZE, 103);
        verify(restClientBuilderMock).property(KEEP_ALIVE_ENABLED, false);
        verify(restClientBuilderMock).property(MAX_REDIRECTS, 104);
        verify(restClientBuilderMock).property(MAX_CHUNK_SIZE, 1024);
        verify(restClientBuilderMock).followRedirects(true);
        verify(restClientBuilderMock).register(MyResponseFilter1.class);
        verify(restClientBuilderMock).queryParamStyle(COMMA_SEPARATED);

        verify(restClientBuilderMock).trustStore(Mockito.any(), Mockito.anyString());
        verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    @Test
    void globalConfigs() {
        RestClientsConfig configRoot = ConfigUtils.emptyConfigBuilder()
                .setAddDefaultSources(false)
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(new RegisteredRestClient(TestClient.class, "test-client"));
                            }
                        }.configBuilder(builder);
                    }
                })
                .withDefaultValues(createSampleConfigRoot())
                .build()
                .getConfigMapping(RestClientsConfig.class);

        QuarkusRestClientBuilderImpl restClientBuilderMock = Mockito.mock(QuarkusRestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).configureBuilder(restClientBuilderMock);

        assertEquals(1, configRoot.clients().size());
        assertTrue(configRoot.clients().containsKey(TestClient.class.getName()));

        verify(restClientBuilderMock).baseUri(URI.create("http://localhost:8080"));
        verify(restClientBuilderMock).property(MULTIPART_ENCODER_MODE, HTML5);
        verify(restClientBuilderMock).property(DISABLE_CONTEXTUAL_ERROR_MESSAGES, true);

        verify(restClientBuilderMock).proxyAddress("host2", 123);
        verify(restClientBuilderMock).proxyUser("proxyUser2");
        verify(restClientBuilderMock).proxyPassword("proxyPassword2");
        verify(restClientBuilderMock).nonProxyHosts("nonProxyHosts2");
        verify(restClientBuilderMock).connectTimeout(200, MILLISECONDS);
        verify(restClientBuilderMock).readTimeout(201, MILLISECONDS);
        verify(restClientBuilderMock).userAgent("agent2");
        verify(restClientBuilderMock).property(STATIC_HEADERS, Map.of("header2", "value"));
        verify(restClientBuilderMock).property(CONNECTION_TTL, 20);
        verify(restClientBuilderMock).property(CONNECTION_POOL_SIZE, 203);
        verify(restClientBuilderMock).property(KEEP_ALIVE_ENABLED, true);
        verify(restClientBuilderMock).property(MAX_REDIRECTS, 204);
        verify(restClientBuilderMock).property(MAX_CHUNK_SIZE, 1024);
        verify(restClientBuilderMock).followRedirects(true);
        verify(restClientBuilderMock).register(MyResponseFilter2.class);
        verify(restClientBuilderMock).queryParamStyle(MULTI_PAIRS);

        verify(restClientBuilderMock).trustStore(Mockito.any(), Mockito.anyString());
        verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    private static Map<String, String> createSampleConfigRoot() {
        Map<String, String> rootConfig = new HashMap<>();
        // global properties:
        rootConfig.put("quarkus.rest-client.multipart-post-encoder-mode", "HTML5");
        rootConfig.put("quarkus.rest-client.disable-contextual-error-messages", "true");
        // global defaults for client specific properties:
        rootConfig.put("quarkus.rest-client.proxy-address", "host2:123");
        rootConfig.put("quarkus.rest-client.proxy-user", "proxyUser2");
        rootConfig.put("quarkus.rest-client.proxy-password", "proxyPassword2");
        rootConfig.put("quarkus.rest-client.non-proxy-hosts", "nonProxyHosts2");
        rootConfig.put("quarkus.rest-client.connect-timeout", "200");
        rootConfig.put("quarkus.rest-client.read-timeout", "201");
        rootConfig.put("quarkus.rest-client.user-agent", "agent2");
        rootConfig.put("quarkus.rest-client.headers.header2", "value");
        rootConfig.put("quarkus.rest-client.connection-ttl", "20000");
        rootConfig.put("quarkus.rest-client.connection-pool-size", "203");
        rootConfig.put("quarkus.rest-client.keep-alive-enabled", "true");
        rootConfig.put("quarkus.rest-client.max-redirects", "204");
        rootConfig.put("quarkus.rest-client.multipart-max-chunk-size", "1024");
        rootConfig.put("quarkus.rest-client.follow-redirects", "true");
        rootConfig.put("quarkus.rest-client.max-chunk-size", "1024");
        rootConfig.put("quarkus.rest-client.providers",
                "io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter2");
        rootConfig.put("quarkus.rest-client.query-param-style", "multi-pairs");
        rootConfig.put("quarkus.rest-client.trust-store", truststorePath.toAbsolutePath().toString());
        rootConfig.put("quarkus.rest-client.trust-store-password", "truststorePassword");
        rootConfig.put("quarkus.rest-client.trust-store-type", "JKS");
        rootConfig.put("quarkus.rest-client.key-store", keystorePath.toAbsolutePath().toString());
        rootConfig.put("quarkus.rest-client.key-store-password", "keystorePassword");
        rootConfig.put("quarkus.rest-client.key-store-type", "JKS");
        return rootConfig;
    }

    private static Map<String, String> createSampleClientConfig(final String restClientName) {
        Map<String, String> clientConfig = new HashMap<>();
        // properties only configurable via client config
        clientConfig.put("quarkus.rest-client." + restClientName + ".url", "http://localhost");
        clientConfig.put("quarkus.rest-client." + restClientName + ".uri", "");
        clientConfig.put("quarkus.rest-client." + restClientName + ".shared", "true");
        clientConfig.put("quarkus.rest-client." + restClientName + ".name", "my-client");
        // properties that override configRoot counterparts
        clientConfig.put("quarkus.rest-client." + restClientName + ".proxy-address", "host1:123");
        clientConfig.put("quarkus.rest-client." + restClientName + ".proxy-user", "proxyUser1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".proxy-password", "proxyPassword1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".non-proxy-hosts", "nonProxyHosts1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".connect-timeout", "100");
        clientConfig.put("quarkus.rest-client." + restClientName + ".read-timeout", "101");
        clientConfig.put("quarkus.rest-client." + restClientName + ".user-agent", "agent1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".headers.header1", "value");
        clientConfig.put("quarkus.rest-client." + restClientName + ".connection-ttl", "10000");
        clientConfig.put("quarkus.rest-client." + restClientName + ".connection-pool-size", "103");
        clientConfig.put("quarkus.rest-client." + restClientName + ".keep-alive-enabled", "false");
        clientConfig.put("quarkus.rest-client." + restClientName + ".max-redirects", "104");
        clientConfig.put("quarkus.rest-client." + restClientName + ".follow-redirects", "true");
        clientConfig.put("quarkus.rest-client." + restClientName + ".max-chunk-size", "1024");
        clientConfig.put("quarkus.rest-client." + restClientName + ".providers",
                "io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".query-param-style", "comma-separated");
        clientConfig.put("quarkus.rest-client." + restClientName + ".trust-store", truststorePath.toAbsolutePath().toString());
        clientConfig.put("quarkus.rest-client." + restClientName + ".trust-store-password", "truststorePassword");
        clientConfig.put("quarkus.rest-client." + restClientName + ".trust-store-type", "JKS");
        clientConfig.put("quarkus.rest-client." + restClientName + ".key-store", keystorePath.toAbsolutePath().toString());
        clientConfig.put("quarkus.rest-client." + restClientName + ".key-store-password", "keystorePassword");
        clientConfig.put("quarkus.rest-client." + restClientName + ".key-store-type", "JKS");
        return clientConfig;
    }

    @RegisterRestClient(configKey = "test-client")
    interface TestClient {
        @SuppressWarnings("unused")
        String methodA();
    }

    public static class MyResponseFilter1 implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        }
    }

    public static class MyResponseFilter2 implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        }
    }

}
