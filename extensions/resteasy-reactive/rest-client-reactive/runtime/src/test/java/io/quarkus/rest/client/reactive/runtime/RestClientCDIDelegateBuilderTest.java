package io.quarkus.rest.client.reactive.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientMultipartConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.runtime.configuration.MemorySize;

@SuppressWarnings({ "SameParameterValue" })
public class RestClientCDIDelegateBuilderTest {

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
    public void testClientSpecificConfigs() {
        // given

        RestClientsConfig configRoot = createSampleConfigRoot();
        configRoot.putClientConfig("test-client", createSampleClientConfig());

        // when

        QuarkusRestClientBuilderImpl restClientBuilderMock = Mockito.mock(QuarkusRestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).configureBuilder(restClientBuilderMock);

        // then

        Mockito.verify(restClientBuilderMock).baseUri(URI.create("http://localhost"));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.SHARED, true);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.NAME, "my-client");
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE,
                PausableHttpPostRequestEncoder.EncoderMode.HTML5);

        Mockito.verify(restClientBuilderMock).proxyAddress("host1", 123);
        Mockito.verify(restClientBuilderMock).proxyUser("proxyUser1");
        Mockito.verify(restClientBuilderMock).proxyPassword("proxyPassword1");
        Mockito.verify(restClientBuilderMock).nonProxyHosts("nonProxyHosts1");
        Mockito.verify(restClientBuilderMock).connectTimeout(100, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(101, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.USER_AGENT, "agent1");
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.STATIC_HEADERS,
                Collections.singletonMap("header1", "value"));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_TTL, 10); // value converted to seconds
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 103);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.KEEP_ALIVE_ENABLED, false);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_REDIRECTS, 104);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_CHUNK_SIZE, 1024);
        Mockito.verify(restClientBuilderMock).followRedirects(true);
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter1.class);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.COMMA_SEPARATED);

        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any(), Mockito.anyString());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testGlobalConfigs() {
        // given

        RestClientsConfig configRoot = createSampleConfigRoot();

        // when

        QuarkusRestClientBuilderImpl restClientBuilderMock = Mockito.mock(QuarkusRestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).configureBuilder(restClientBuilderMock);

        // then

        Mockito.verify(restClientBuilderMock).baseUri(URI.create("http://localhost:8080"));
        Mockito.verify(restClientBuilderMock)
                .property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, PausableHttpPostRequestEncoder.EncoderMode.HTML5);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.DISABLE_CONTEXTUAL_ERROR_MESSAGES, true);

        Mockito.verify(restClientBuilderMock).proxyAddress("host2", 123);
        Mockito.verify(restClientBuilderMock).proxyUser("proxyUser2");
        Mockito.verify(restClientBuilderMock).proxyPassword("proxyPassword2");
        Mockito.verify(restClientBuilderMock).nonProxyHosts("nonProxyHosts2");
        Mockito.verify(restClientBuilderMock).connectTimeout(200, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(201, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.USER_AGENT, "agent2");
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.STATIC_HEADERS,
                Collections.singletonMap("header2", "value"));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_TTL, 20);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 203);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.KEEP_ALIVE_ENABLED, true);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_REDIRECTS, 204);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_CHUNK_SIZE, 1024);
        Mockito.verify(restClientBuilderMock).followRedirects(true);
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter2.class);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.MULTI_PAIRS);

        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any(), Mockito.anyString());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    private static RestClientsConfig createSampleConfigRoot() {
        RestClientsConfig configRoot = new RestClientsConfig();

        // global properties:
        configRoot.multipartPostEncoderMode = Optional.of("HTML5");
        configRoot.disableContextualErrorMessages = true;

        // global defaults for client specific properties:
        configRoot.proxyAddress = Optional.of("host2:123");
        configRoot.proxyUser = Optional.of("proxyUser2");
        configRoot.proxyPassword = Optional.of("proxyPassword2");
        configRoot.nonProxyHosts = Optional.of("nonProxyHosts2");
        configRoot.connectTimeout = 200L;
        configRoot.readTimeout = 201L;
        configRoot.userAgent = Optional.of("agent2");
        configRoot.headers = Collections.singletonMap("header2", "value");
        configRoot.connectionTTL = Optional.of(20000); // value in ms, will be converted to seconds
        configRoot.connectionPoolSize = Optional.of(203);
        configRoot.keepAliveEnabled = Optional.of(true);
        configRoot.maxRedirects = Optional.of(204);
        configRoot.multipart = new RestClientMultipartConfig();
        configRoot.multipart.maxChunkSize = Optional.of(1024);
        configRoot.followRedirects = Optional.of(true);
        configRoot.maxChunkSize = Optional.of(new MemorySize(BigInteger.valueOf(1024)));
        configRoot.providers = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter2");
        configRoot.queryParamStyle = Optional.of(QueryParamStyle.MULTI_PAIRS);

        configRoot.trustStore = Optional.of(truststorePath.toAbsolutePath().toString());
        configRoot.trustStorePassword = Optional.of("truststorePassword");
        configRoot.trustStoreType = Optional.of("JKS");
        configRoot.keyStore = Optional.of(keystorePath.toAbsolutePath().toString());
        configRoot.keyStorePassword = Optional.of("keystorePassword");
        configRoot.keyStoreType = Optional.of("JKS");

        return configRoot;
    }

    private static RestClientConfig createSampleClientConfig() {
        RestClientConfig clientConfig = new RestClientConfig();

        // properties only configurable via client config
        clientConfig.url = Optional.of("http://localhost");
        clientConfig.uri = Optional.empty();
        clientConfig.shared = Optional.of(true);
        clientConfig.name = Optional.of("my-client");

        // properties that override configRoot counterparts
        clientConfig.proxyAddress = Optional.of("host1:123");
        clientConfig.proxyUser = Optional.of("proxyUser1");
        clientConfig.proxyPassword = Optional.of("proxyPassword1");
        clientConfig.nonProxyHosts = Optional.of("nonProxyHosts1");
        clientConfig.connectTimeout = Optional.of(100L);
        clientConfig.readTimeout = Optional.of(101L);
        clientConfig.userAgent = Optional.of("agent1");
        clientConfig.headers = Collections.singletonMap("header1", "value");
        clientConfig.connectionTTL = Optional.of(10000); // value in milliseconds, will be converted to seconds
        clientConfig.connectionPoolSize = Optional.of(103);
        clientConfig.keepAliveEnabled = Optional.of(false);
        clientConfig.maxRedirects = Optional.of(104);
        clientConfig.followRedirects = Optional.of(true);
        clientConfig.multipart = new RestClientMultipartConfig();
        clientConfig.maxChunkSize = Optional.of(new MemorySize(BigInteger.valueOf(1024)));
        clientConfig.providers = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter1");
        clientConfig.queryParamStyle = Optional.of(QueryParamStyle.COMMA_SEPARATED);

        clientConfig.trustStore = Optional.of(truststorePath.toAbsolutePath().toString());
        clientConfig.trustStorePassword = Optional.of("truststorePassword");
        clientConfig.trustStoreType = Optional.of("JKS");
        clientConfig.keyStore = Optional.of(keystorePath.toAbsolutePath().toString());
        clientConfig.keyStorePassword = Optional.of("keystorePassword");
        clientConfig.keyStoreType = Optional.of("JKS");

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
