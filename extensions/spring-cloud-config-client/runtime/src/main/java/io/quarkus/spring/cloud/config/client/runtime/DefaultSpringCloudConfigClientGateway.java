package io.quarkus.spring.cloud.config.client.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultSpringCloudConfigClientGateway implements SpringCloudConfigClientGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final SpringCloudConfigClientConfig springCloudConfigClientConfig;
    private final SSLConnectionSocketFactory sslSocketFactory;
    private final URI baseURI;

    public DefaultSpringCloudConfigClientGateway(SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        this.springCloudConfigClientConfig = springCloudConfigClientConfig;
        try {
            this.baseURI = determineBaseUri(springCloudConfigClientConfig);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + springCloudConfigClientConfig.url
                    + "' of property 'quarkus.spring-cloud-config.url' is invalid", e);
        }

        if (springCloudConfigClientConfig.trustStore.isPresent() || springCloudConfigClientConfig.keyStore.isPresent()
                || springCloudConfigClientConfig.trustCerts) {
            this.sslSocketFactory = createFactoryFromAgentConfig(springCloudConfigClientConfig);
        } else {
            this.sslSocketFactory = null;
        }
    }

    private URI determineBaseUri(SpringCloudConfigClientConfig springCloudConfigClientConfig) throws URISyntaxException {
        String url = springCloudConfigClientConfig.url;
        if (null == url || url.isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'quarkus.spring-cloud-config.url' property cannot be empty");
        }
        if (url.endsWith("/")) {
            return new URI(url.substring(0, url.length() - 1));
        }
        return new URI(url);
    }

    // The SSL code is basically a copy of the code in the Consul extension
    // Normally we would consider moving this code to one place, but as I want
    // to stop using Apache HTTP Client when we move to JDK 11, lets not do the
    // extra work

    private SSLConnectionSocketFactory createFactoryFromAgentConfig(
            SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (springCloudConfigClientConfig.trustStore.isPresent()) {
                sslContextBuilder = sslContextBuilder
                        .loadTrustMaterial(readStore(springCloudConfigClientConfig.trustStore.get(),
                                springCloudConfigClientConfig.trustStorePassword), null);
            } else if (springCloudConfigClientConfig.trustCerts) {
                sslContextBuilder = sslContextBuilder.loadTrustMaterial(TrustAllStrategy.INSTANCE);
            }
            if (springCloudConfigClientConfig.keyStore.isPresent()) {
                String keyPassword = springCloudConfigClientConfig.keyPassword
                        .orElse(springCloudConfigClientConfig.keyStorePassword.orElse(""));
                sslContextBuilder = sslContextBuilder.loadKeyMaterial(
                        readStore(springCloudConfigClientConfig.keyStore.get(), springCloudConfigClientConfig.keyStorePassword),
                        keyPassword.toCharArray());
            }
            return new SSLConnectionSocketFactory(sslContextBuilder.build(), NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | IOException | CertificateException
                | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findKeystoreFileType(Path keyStorePath) {
        String pathName = keyStorePath.toString().toLowerCase();
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return "PKS12";
        }
        return "JKS";
    }

    private static KeyStore readStore(Path keyStorePath, Optional<String> keyStorePassword)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        String keyStoreType = findKeystoreFileType(keyStorePath);

        InputStream classPathResource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(keyStorePath.toString());
        if (classPathResource != null) {
            try (InputStream is = classPathResource) {
                return doReadStore(is, keyStoreType, keyStorePassword);
            }
        } else {
            try (InputStream is = Files.newInputStream(keyStorePath)) {
                return doReadStore(is, keyStoreType, keyStorePassword);
            }
        }
    }

    private static KeyStore doReadStore(InputStream keyStoreStream, String keyStoreType, Optional<String> keyStorePassword)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(keyStoreStream, keyStorePassword.isPresent() ? keyStorePassword.get().toCharArray() : null);
        return keyStore;
    }

    @Override
    public Response exchange(String applicationName, String profile) throws Exception {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout((int) springCloudConfigClientConfig.connectionTimeout.toMillis())
                .setSocketTimeout((int) springCloudConfigClientConfig.readTimeout.toMillis())
                .build();

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
        if (sslSocketFactory != null) {
            httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
        }

        try (CloseableHttpClient client = httpClientBuilder.build()) {
            final URI finalURI = finalURI(applicationName, profile);
            final HttpGet request = new HttpGet(finalURI);
            request.addHeader("Accept", "application/json");

            for (Map.Entry<String, String> entry : springCloudConfigClientConfig.headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }

            HttpClientContext context = setupContext(finalURI);
            try (CloseableHttpResponse response = client.execute(request, context)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Got unexpected HTTP response code " + response.getStatusLine().getStatusCode()
                            + " from " + finalURI);
                }
                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new RuntimeException("Got empty HTTP response body " + finalURI);
                }

                return OBJECT_MAPPER.readValue(EntityUtils.toString(entity), Response.class);
            }
        }
    }

    private URI finalURI(String applicationName, String profile) throws URISyntaxException {
        URIBuilder result = new URIBuilder(baseURI);
        if (result.getPort() == -1) {
            // we need to set the port otherwise auth case doesn't match the request
            result.setPort(result.getScheme().equalsIgnoreCase("http") ? 80 : 443);
        }
        List<String> finalPathSegments = new ArrayList<>(result.getPathSegments());
        finalPathSegments.add(applicationName);
        finalPathSegments.add(profile);
        if (springCloudConfigClientConfig.label.isPresent()) {
            finalPathSegments.add(springCloudConfigClientConfig.label.get());
        }
        result.setPathSegments(finalPathSegments);
        return result.build();
    }

    private HttpClientContext setupContext(URI finalURI) {
        final HttpClientContext context = HttpClientContext.create();
        if ((baseURI.getUserInfo() != null) || springCloudConfigClientConfig.usernameAndPasswordSet()) {
            final AuthCache authCache = InMemoryAuthCache.INSTANCE;
            authCache.put(new HttpHost(finalURI.getHost(), finalURI.getPort(), finalURI.getScheme()), new BasicScheme());
            context.setAuthCache(authCache);
            if (springCloudConfigClientConfig.usernameAndPasswordSet()) {
                final CredentialsProvider provider = new BasicCredentialsProvider();
                final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                        springCloudConfigClientConfig.username.get(), springCloudConfigClientConfig.password.get());
                provider.setCredentials(AuthScope.ANY, credentials);
                context.setCredentialsProvider(provider);
            }
        }
        return context;
    }

    /**
     * We need this class in order to avoid the serialization that Apache HTTP client does by default
     * and that does not work in GraalVM.
     * We don't care about caching the auth result since one call is only ever going to be made in any case
     */
    private static class InMemoryAuthCache implements AuthCache {

        static final InMemoryAuthCache INSTANCE = new InMemoryAuthCache();

        private final Map<HttpHost, AuthScheme> map = new ConcurrentHashMap<>();

        private InMemoryAuthCache() {
        }

        @Override
        public void put(HttpHost host, AuthScheme authScheme) {
            map.put(host, authScheme);
        }

        @Override
        public AuthScheme get(HttpHost host) {
            return map.get(host);
        }

        @Override
        public void remove(HttpHost host) {
            map.remove(host);
        }

        @Override
        public void clear() {
            map.clear();
        }
    };
}
