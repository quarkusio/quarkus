package io.quarkus.rest.client.reactive.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;

@SuppressWarnings({ "SameParameterValue" })
public class RestClientCDIDelegateBuilderTest {

    private static final String TRUSTSTORE_PASSWORD = "truststorePassword";
    private static final String KEYSTORE_PASSWORD = "keystorePassword";

    @TempDir
    static File tempDir;
    private static File truststoreFile;
    private static File keystoreFile;
    private static Config createdConfig;

    @BeforeAll
    public static void beforeAll() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        // prepare keystore and truststore

        truststoreFile = new File(tempDir, "truststore.jks");
        keystoreFile = new File(tempDir, "keystore.jks");

        KeyStore truststore = KeyStore.getInstance("JKS");
        truststore.load(null, TRUSTSTORE_PASSWORD.toCharArray());
        truststore.store(new FileOutputStream(truststoreFile), TRUSTSTORE_PASSWORD.toCharArray());

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, KEYSTORE_PASSWORD.toCharArray());
        keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_PASSWORD.toCharArray());
    }

    @AfterEach
    public void afterEach() {
        if (createdConfig != null) {
            ConfigProviderResolver.instance().releaseConfig(createdConfig);
            createdConfig = null;
        }
    }

    @Test
    public void testClientSpecificConfigs() {
        // given

        RestClientsConfig configRoot = createSampleConfigRoot();
        configRoot.putClientConfig("test-client", createSampleClientConfig());

        // when

        RestClientBuilderImpl restClientBuilderMock = Mockito.mock(RestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).configureBuilder(restClientBuilderMock);

        // then

        Mockito.verify(restClientBuilderMock).baseUri(URI.create("http://localhost"));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.SHARED, true);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.NAME, "my-client");
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE,
                HttpPostRequestEncoder.EncoderMode.HTML5);

        Mockito.verify(restClientBuilderMock).proxyAddress("host1", 123);
        Mockito.verify(restClientBuilderMock).proxyUser("proxyUser1");
        Mockito.verify(restClientBuilderMock).proxyPassword("proxyPassword1");
        Mockito.verify(restClientBuilderMock).nonProxyHosts("nonProxyHosts1");
        Mockito.verify(restClientBuilderMock).connectTimeout(100, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(101, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.USER_AGENT, "agent1");
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.STATIC_HEADERS,
                Collections.singletonMap("header1", "value"));
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier1.class));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_TTL, 10); // value converted to seconds
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 103);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_REDIRECTS, 104);
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

        RestClientBuilderImpl restClientBuilderMock = Mockito.mock(RestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).configureBuilder(restClientBuilderMock);

        // then

        Mockito.verify(restClientBuilderMock).baseUri(URI.create("http://localhost:8080"));
        Mockito.verify(restClientBuilderMock)
                .property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, HttpPostRequestEncoder.EncoderMode.HTML5);
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
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier2.class));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_TTL, 20);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 203);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_REDIRECTS, 204);
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
        configRoot.hostnameVerifier = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyHostnameVerifier2");
        configRoot.connectionTTL = Optional.of(20000); // value in ms, will be converted to seconds
        configRoot.connectionPoolSize = Optional.of(203);
        configRoot.maxRedirects = Optional.of(204);
        configRoot.followRedirects = Optional.of(true);
        configRoot.providers = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter2");
        configRoot.queryParamStyle = Optional.of(QueryParamStyle.MULTI_PAIRS);

        configRoot.trustStore = Optional.of(truststoreFile.getAbsolutePath());
        configRoot.trustStorePassword = Optional.of("truststorePassword");
        configRoot.trustStoreType = Optional.of("JKS");
        configRoot.keyStore = Optional.of(keystoreFile.getAbsolutePath());
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
        clientConfig.hostnameVerifier = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyHostnameVerifier1");
        clientConfig.connectionTTL = Optional.of(10000); // value in milliseconds, will be converted to seconds
        clientConfig.connectionPoolSize = Optional.of(103);
        clientConfig.maxRedirects = Optional.of(104);
        clientConfig.followRedirects = Optional.of(true);
        clientConfig.providers = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter1");
        clientConfig.queryParamStyle = Optional.of(QueryParamStyle.COMMA_SEPARATED);

        clientConfig.trustStore = Optional.of(truststoreFile.getAbsolutePath());
        clientConfig.trustStorePassword = Optional.of("truststorePassword");
        clientConfig.trustStoreType = Optional.of("JKS");
        clientConfig.keyStore = Optional.of(keystoreFile.getAbsolutePath());
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
