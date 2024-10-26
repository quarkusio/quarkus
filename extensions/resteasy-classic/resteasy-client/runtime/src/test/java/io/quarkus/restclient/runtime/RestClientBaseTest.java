package io.quarkus.restclient.runtime;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.microprofile.rest.client.ext.QueryParamStyle.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
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

class RestClientBaseTest {

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
    void clientSpecificConfigs() throws Exception {
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

        RestClientBuilder restClientBuilderMock = Mockito.mock(RestClientBuilder.class);
        RestClientBase restClientBase = new RestClientBase(TestClient.class,
                "http://localhost:8080",
                "test-client",
                null,
                configRoot);
        restClientBase.configureBuilder(restClientBuilderMock);

        verify(restClientBuilderMock).baseUrl(new URL("http://localhost:8080"));
        verify(restClientBuilderMock).proxyAddress("host1", 123);
        verify(restClientBuilderMock).connectTimeout(100, MILLISECONDS);
        verify(restClientBuilderMock).readTimeout(101, MILLISECONDS);
        verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier1.class));
        verify(restClientBuilderMock).property("resteasy.connectionTTL", Arrays.asList(102, MILLISECONDS));
        verify(restClientBuilderMock).property("resteasy.connectionPoolSize", 103);
        verify(restClientBuilderMock).followRedirects(true);
        verify(restClientBuilderMock).register(MyResponseFilter1.class);
        verify(restClientBuilderMock).queryParamStyle(COMMA_SEPARATED);
        verify(restClientBuilderMock).trustStore(Mockito.any());
        verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    @Test
    void globalConfigs() throws MalformedURLException {
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

        assertEquals(1, configRoot.clients().size());
        assertTrue(configRoot.clients().containsKey(TestClient.class.getName()));

        RestClientBuilder restClientBuilderMock = Mockito.mock(RestClientBuilder.class);
        RestClientBase restClientBase = new RestClientBase(TestClient.class,
                "http://localhost:8080",
                "test-client",
                null,
                configRoot);
        restClientBase.configureBuilder(restClientBuilderMock);

        // then
        verify(restClientBuilderMock).baseUrl(new URL("http://localhost:8080"));
        verify(restClientBuilderMock).proxyAddress("host2", 123);
        verify(restClientBuilderMock).connectTimeout(200, MILLISECONDS);
        verify(restClientBuilderMock).readTimeout(201, MILLISECONDS);
        verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier2.class));
        verify(restClientBuilderMock).property("resteasy.connectionTTL", Arrays.asList(202, MILLISECONDS));
        verify(restClientBuilderMock).property("resteasy.connectionPoolSize", 203);
        verify(restClientBuilderMock).followRedirects(true);
        verify(restClientBuilderMock).register(MyResponseFilter2.class);
        verify(restClientBuilderMock).queryParamStyle(MULTI_PAIRS);
        verify(restClientBuilderMock).trustStore(Mockito.any());
        verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    private static Map<String, String> createSampleConfigRoot() {
        Map<String, String> rootConfig = new HashMap<>();
        rootConfig.put("quarkus.rest-client.proxy-address", "host2:123");
        rootConfig.put("quarkus.rest-client.connect-timeout", "200");
        rootConfig.put("quarkus.rest-client.read-timeout", "201");
        rootConfig.put("quarkus.rest-client.hostname-verifier",
                "io.quarkus.restclient.runtime.RestClientBaseTest$MyHostnameVerifier2");
        rootConfig.put("quarkus.rest-client.connection-ttl", "202");
        rootConfig.put("quarkus.rest-client.connection-pool-size", "203");
        rootConfig.put("quarkus.rest-client.follow-redirects", "true");
        rootConfig.put("quarkus.rest-client.providers", "io.quarkus.restclient.runtime.RestClientBaseTest$MyResponseFilter2");
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
        // properties that override configRoot counterparts
        clientConfig.put("quarkus.rest-client." + restClientName + ".proxy-address", "host1:123");
        clientConfig.put("quarkus.rest-client." + restClientName + ".connect-timeout", "100");
        clientConfig.put("quarkus.rest-client." + restClientName + ".read-timeout", "101");
        clientConfig.put("quarkus.rest-client." + restClientName + ".hostname-verifier",
                "io.quarkus.restclient.runtime.RestClientBaseTest$MyHostnameVerifier1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".connection-ttl", "102");
        clientConfig.put("quarkus.rest-client." + restClientName + ".connection-pool-size", "103");
        clientConfig.put("quarkus.rest-client." + restClientName + ".follow-redirects", "true");
        clientConfig.put("quarkus.rest-client." + restClientName + ".providers",
                "io.quarkus.restclient.runtime.RestClientBaseTest$MyResponseFilter1");
        clientConfig.put("quarkus.rest-client." + restClientName + ".query-param-style", "comma-separated");
        clientConfig.put("quarkus.rest-client." + restClientName + ".trust-store", truststorePath.toAbsolutePath().toString());
        clientConfig.put("quarkus.rest-client." + restClientName + ".trust-store-password", "truststorePassword");
        clientConfig.put("quarkus.rest-client." + restClientName + ".trust-store-type", "JKS");
        clientConfig.put("quarkus.rest-client." + restClientName + ".key-store", keystorePath.toAbsolutePath().toString());
        clientConfig.put("quarkus.rest-client." + restClientName + ".key-store-password", "keystorePassword");
        clientConfig.put("quarkus.rest-client." + restClientName + ".key-store-type", "JKS");
        return clientConfig;
    }

    @RegisterRestClient
    interface TestClient {
        String echo(String message);
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

    public static class MyHostnameVerifier1 implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static class MyHostnameVerifier2 implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
