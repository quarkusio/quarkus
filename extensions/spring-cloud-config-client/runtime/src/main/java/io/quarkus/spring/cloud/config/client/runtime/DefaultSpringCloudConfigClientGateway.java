package io.quarkus.spring.cloud.config.client.runtime;

import java.io.IOException;
import java.util.Map;
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
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultSpringCloudConfigClientGateway implements SpringCloudConfigClientGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final SpringCloudConfigClientConfig springCloudConfigClientConfig;
    private final String baseUri;

    public DefaultSpringCloudConfigClientGateway(SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        this.springCloudConfigClientConfig = springCloudConfigClientConfig;
        this.baseUri = determineBaseUri(springCloudConfigClientConfig);
    }

    private String determineBaseUri(SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        String baseUri = springCloudConfigClientConfig.url;
        if (null == baseUri || baseUri.isEmpty()) {
            throw new IllegalArgumentException("baseUri cannot be empty");
        }
        if (baseUri.endsWith("/")) {
            return baseUri.substring(0, baseUri.length() - 1);
        }
        return baseUri;
    }

    @Override
    public Response exchange(String applicationName, String profile) throws IOException {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout((int) springCloudConfigClientConfig.connectionTimeout.toMillis())
                .setSocketTimeout((int) springCloudConfigClientConfig.readTimeout.toMillis())
                .build();
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
        try (CloseableHttpClient client = httpClientBuilder.build()) {
            final String finalUri = baseUri + "/" + applicationName + "/" + profile;
            final HttpGet request = new HttpGet(finalUri);
            request.addHeader("Accept", "application/json");

            HttpClientContext context = setupContext();
            try (CloseableHttpResponse response = client.execute(request, context)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Got unexpected HTTP response code " + response.getStatusLine().getStatusCode()
                            + " from " + finalUri);
                }
                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new RuntimeException("Got empty HTTP response body " + finalUri);
                }

                return OBJECT_MAPPER.readValue(EntityUtils.toString(entity), Response.class);
            }
        }
    }

    private HttpClientContext setupContext() {
        final HttpClientContext context = HttpClientContext.create();
        if (baseUri.contains("@") || springCloudConfigClientConfig.usernameAndPasswordSet()) {
            final AuthCache authCache = InMemoryAuthCache.INSTANCE;
            authCache.put(HttpHost.create(baseUri), new BasicScheme());
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
