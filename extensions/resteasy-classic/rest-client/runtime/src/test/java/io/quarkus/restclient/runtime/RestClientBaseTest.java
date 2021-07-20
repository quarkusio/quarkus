package io.quarkus.restclient.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientConfigRoot;
import io.smallrye.config.PropertiesConfigSource;

public class RestClientBaseTest {

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
    public void testQuarkusConfig() throws Exception {
        // legacy configuration should be overridden by the one configured bellow
        setupMpConfig("mp-style-application.properties");

        RestClientConfigRoot configRoot = createSampleConfiguration();
        RestClientBuilder restClientBuilderMock = Mockito.mock(RestClientBuilder.class);
        RestClientBase restClientBase = new RestClientBase(TestClient.class,
                "http://localhost:8080",
                "test-client",
                null,
                configRoot);
        restClientBase.configureBaseUrl(restClientBuilderMock);
        restClientBase.configureTimeouts(restClientBuilderMock);
        restClientBase.configureProviders(restClientBuilderMock);
        restClientBase.configureSsl(restClientBuilderMock);
        restClientBase.configureProxy(restClientBuilderMock);
        restClientBase.configureRedirects(restClientBuilderMock);
        restClientBase.configureQueryParamStyle(restClientBuilderMock);
        restClientBase.configureCustomProperties(restClientBuilderMock);

        Mockito.verify(restClientBuilderMock).baseUrl(new URL("http://localhost"));
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter1.class);
        Mockito.verify(restClientBuilderMock).connectTimeout(100, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(101, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).followRedirects(true);
        Mockito.verify(restClientBuilderMock).proxyAddress("localhost", 1234);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.COMMA_SEPARATED);
        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier1.class));
        Mockito.verify(restClientBuilderMock).property("resteasy.connectionTTL", Arrays.asList(102, TimeUnit.MILLISECONDS));
        Mockito.verify(restClientBuilderMock).property("resteasy.connectionPoolSize", 103);
    }

    @Test
    public void testLegacyConfig() throws Exception {
        Properties properties = new Properties();
        properties.put("test-client/mp-rest/trustStore", truststoreFile.getAbsolutePath());
        properties.put("test-client/mp-rest/keyStore", keystoreFile.getAbsolutePath());
        setupMpConfig("mp-style-application.properties", properties);

        RestClientBuilder restClientBuilderMock = Mockito.mock(RestClientBuilder.class);
        RestClientBase restClientBase = new RestClientBase(TestClient.class,
                "http://localhost:8080",
                "test-client",
                null,
                createEmptyConfiguration());
        restClientBase.configureBaseUrl(restClientBuilderMock);
        restClientBase.configureTimeouts(restClientBuilderMock);
        restClientBase.configureProviders(restClientBuilderMock);
        restClientBase.configureSsl(restClientBuilderMock);
        restClientBase.configureProxy(restClientBuilderMock);
        restClientBase.configureRedirects(restClientBuilderMock);
        restClientBase.configureQueryParamStyle(restClientBuilderMock);
        restClientBase.configureCustomProperties(restClientBuilderMock);

        Mockito.verify(restClientBuilderMock).baseUrl(new URL("http://localhost:8080"));
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter2.class);
        Mockito.verify(restClientBuilderMock).connectTimeout(1, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(2, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).followRedirects(false);
        Mockito.verify(restClientBuilderMock).proxyAddress("localhost", 8081);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.ARRAY_PAIRS);
        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier2.class));
    }

    /**
     * This method creates a MicroProfile config accessible via `ConfigProvider.getConfig()`.
     */
    @SuppressWarnings("SameParameterValue")
    private static void setupMpConfig(String propertiesFile, Properties... properties) throws IOException {
        URL propertiesURL = RestClientBaseTest.class.getClassLoader().getResource(propertiesFile);
        assertThat(propertiesURL).isNotNull();

        ConfigBuilder configBuilder = ConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(propertiesURL));
        for (Properties prop : properties) {
            configBuilder.withSources(new PropertiesConfigSource(prop, null));
        }
        createdConfig = configBuilder.build();

        ConfigProviderResolver.instance().registerConfig(createdConfig, Thread.currentThread().getContextClassLoader());
    }

    /**
     * This method creates a Quarkus style configuration object (which would normally be created based on the MP config
     * properties, but it's easier to instantiate it directly here).
     */
    private static RestClientConfigRoot createSampleConfiguration() {
        RestClientConfig clientConfig = new RestClientConfig();
        clientConfig.url = Optional.of("http://localhost");
        clientConfig.uri = Optional.empty();
        clientConfig.scope = Optional.of("Singleton");
        clientConfig.providers = Optional
                .of("io.quarkus.restclient.runtime.RestClientBaseTest$MyResponseFilter1");
        clientConfig.connectTimeout = Optional.of(100L);
        clientConfig.readTimeout = Optional.of(101L);
        clientConfig.followRedirects = Optional.of(true);
        clientConfig.proxyAddress = Optional.of("localhost:1234");
        clientConfig.queryParamStyle = Optional.of(QueryParamStyle.COMMA_SEPARATED);
        clientConfig.trustStore = Optional.of(truststoreFile.getAbsolutePath());
        clientConfig.trustStorePassword = Optional.of("truststorePassword");
        clientConfig.trustStoreType = Optional.of("JKS");
        clientConfig.keyStore = Optional.of(keystoreFile.getAbsolutePath());
        clientConfig.keyStorePassword = Optional.of("keystorePassword");
        clientConfig.keyStoreType = Optional.of("JKS");
        clientConfig.hostnameVerifier = Optional
                .of("io.quarkus.restclient.runtime.RestClientBaseTest$MyHostnameVerifier1");
        clientConfig.connectionTTL = Optional.of(102);
        clientConfig.connectionPoolSize = Optional.of(103);
        clientConfig.maxRedirects = Optional.of(104);

        RestClientConfigRoot configRoot = new RestClientConfigRoot();
        configRoot.multipartPostEncoderMode = Optional.of("HTML5");
        configRoot.disableSmartProduces = Optional.of(true);
        configRoot.configs = new HashMap<>();
        configRoot.configs.put("test-client", clientConfig);

        return configRoot;
    }

    private static RestClientConfigRoot createEmptyConfiguration() {
        RestClientConfigRoot configRoot = new RestClientConfigRoot();
        configRoot.configs = Collections.emptyMap();
        configRoot.disableSmartProduces = Optional.empty();
        configRoot.multipartPostEncoderMode = Optional.empty();
        return configRoot;
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
