package io.quarkus.spring.cloud.config.client.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.http.client.utils.URIBuilder;
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
    private final URI baseURI;

    public DefaultSpringCloudConfigClientGateway(SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        this.springCloudConfigClientConfig = springCloudConfigClientConfig;
        try {
            this.baseURI = determineBaseUri(springCloudConfigClientConfig);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + springCloudConfigClientConfig.url
                    + "' of property 'quarkus.spring-cloud-config.url' is invalid", e);
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

    @Override
    public Response exchange(String applicationName, String profile) throws Exception {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout((int) springCloudConfigClientConfig.connectionTimeout.toMillis())
                .setSocketTimeout((int) springCloudConfigClientConfig.readTimeout.toMillis())
                .build();
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
        try (CloseableHttpClient client = httpClientBuilder.build()) {
            final URI finalURI = finalURI(applicationName, profile);
            final HttpGet request = new HttpGet(finalURI);
            request.addHeader("Accept", "application/json");

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
