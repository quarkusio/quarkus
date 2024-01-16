package io.quarkus.restclient.runtime;

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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;

public class RestClientBaseTest {

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
    public void testClientSpecificConfigs() throws Exception {
        // given

        RestClientsConfig configRoot = createSampleConfigRoot();
        configRoot.putClientConfig("test-client", createSampleClientConfig());

        // when

        RestClientBuilder restClientBuilderMock = Mockito.mock(RestClientBuilder.class);
        RestClientBase restClientBase = new RestClientBase(TestClient.class,
                "http://localhost:8080",
                "test-client",
                null,
                configRoot);
        restClientBase.configureBuilder(restClientBuilderMock);

        // then

        Mockito.verify(restClientBuilderMock).baseUrl(new URL("http://localhost"));
        Mockito.verify(restClientBuilderMock).proxyAddress("host1", 123);
        Mockito.verify(restClientBuilderMock).connectTimeout(100, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(101, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier1.class));
        Mockito.verify(restClientBuilderMock).property("resteasy.connectionTTL", Arrays.asList(102, TimeUnit.MILLISECONDS));
        Mockito.verify(restClientBuilderMock).property("resteasy.connectionPoolSize", 103);
        Mockito.verify(restClientBuilderMock).followRedirects(true);
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter1.class);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.COMMA_SEPARATED);

        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testGlobalConfigs() throws MalformedURLException {
        // given

        RestClientsConfig configRoot = createSampleConfigRoot();

        // when

        RestClientBuilder restClientBuilderMock = Mockito.mock(RestClientBuilder.class);
        RestClientBase restClientBase = new RestClientBase(TestClient.class,
                "http://localhost:8080",
                "test-client",
                null,
                configRoot);
        restClientBase.configureBuilder(restClientBuilderMock);

        // then

        Mockito.verify(restClientBuilderMock).baseUrl(new URL("http://localhost:8080"));
        Mockito.verify(restClientBuilderMock).proxyAddress("host2", 123);
        Mockito.verify(restClientBuilderMock).connectTimeout(200, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(201, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier2.class));
        Mockito.verify(restClientBuilderMock).property("resteasy.connectionTTL", Arrays.asList(202, TimeUnit.MILLISECONDS));
        Mockito.verify(restClientBuilderMock).property("resteasy.connectionPoolSize", 203);
        Mockito.verify(restClientBuilderMock).followRedirects(true);
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter2.class);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.MULTI_PAIRS);

        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
    }

    private static RestClientsConfig createSampleConfigRoot() {
        RestClientsConfig configRoot = new RestClientsConfig();

        configRoot.proxyAddress = Optional.of("host2:123");
        configRoot.connectTimeout = 200L;
        configRoot.readTimeout = 201L;
        configRoot.hostnameVerifier = Optional.of("io.quarkus.restclient.runtime.RestClientBaseTest$MyHostnameVerifier2");
        configRoot.connectionTTL = Optional.of(202);
        configRoot.connectionPoolSize = Optional.of(203);
        configRoot.followRedirects = Optional.of(true);
        configRoot.providers = Optional.of("io.quarkus.restclient.runtime.RestClientBaseTest$MyResponseFilter2");
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

        // properties that override configRoot counterparts
        clientConfig.proxyAddress = Optional.of("host1:123");
        clientConfig.connectTimeout = Optional.of(100L);
        clientConfig.readTimeout = Optional.of(101L);
        clientConfig.hostnameVerifier = Optional.of("io.quarkus.restclient.runtime.RestClientBaseTest$MyHostnameVerifier1");
        clientConfig.connectionTTL = Optional.of(102);
        clientConfig.connectionPoolSize = Optional.of(103);
        clientConfig.followRedirects = Optional.of(true);
        clientConfig.providers = Optional.of("io.quarkus.restclient.runtime.RestClientBaseTest$MyResponseFilter1");
        clientConfig.queryParamStyle = Optional.of(QueryParamStyle.COMMA_SEPARATED);

        clientConfig.trustStore = Optional.of(truststorePath.toAbsolutePath().toString());
        clientConfig.trustStorePassword = Optional.of("truststorePassword");
        clientConfig.trustStoreType = Optional.of("JKS");
        clientConfig.keyStore = Optional.of(keystorePath.toAbsolutePath().toString());
        clientConfig.keyStorePassword = Optional.of("keystorePassword");
        clientConfig.keyStoreType = Optional.of("JKS");

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
