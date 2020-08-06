package io.quarkus.azure.app.config.client.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultAzureAppConfigClientGateway implements AzureAppConfigClientGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");

    private final AzureAppConfigClientConfig azureAppConfigClientConfig;
    private final URI baseURI;
    private final String credential;
    private final String secret;

    public DefaultAzureAppConfigClientGateway(AzureAppConfigClientConfig azureAppConfigClientConfig) {
        this.azureAppConfigClientConfig = azureAppConfigClientConfig;
        try {
            this.baseURI = determineBaseUri(azureAppConfigClientConfig);
            this.credential = azureAppConfigClientConfig.credential;
            this.secret = azureAppConfigClientConfig.secret;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + azureAppConfigClientConfig.url
                    + "' of property 'quarkus.azure-app-config.url' is invalid", e);
        }
    }

    private URI determineBaseUri(AzureAppConfigClientConfig azureAppConfigClientConfig) throws URISyntaxException {
        String url = azureAppConfigClientConfig.url;
        if (null == url || url.isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'quarkus.azure-app-config.name' property cannot be empty");
        }
        if (url.endsWith("/")) {
            return new URI(url.substring(0, url.length() - 1));
        }
        return new URI(url);
    }

    @Override
    public Response exchange() throws Exception {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout((int) azureAppConfigClientConfig.connectionTimeout.toMillis())
                .setSocketTimeout((int) azureAppConfigClientConfig.readTimeout.toMillis())
                .build();

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
        try (CloseableHttpClient client = httpClientBuilder.build()) {
            final URI finalURI = finalURI();
            final HttpGet request = new HttpGet(finalURI);
            request.addHeader("Accept", "application/json");
            Map<String, String> authHeaders = generateHeader(request, credential, secret);
            authHeaders.forEach(request::setHeader);

            try (CloseableHttpResponse response = client.execute(request)) {
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

    private URI finalURI() throws URISyntaxException {
        URIBuilder result = new URIBuilder(baseURI);

        result.setPath("/kv");
        result.setParameter("key", "*");
        result.setParameter("api-version", "1.0");
        // result.setParameter("label", "value");
        return result.build();
    }

    /**
     * Generates the required headers to authenticate
     * see https://github.com/Azure/AppConfiguration/blob/master/docs/REST/authentication/hmac.md
     * 
     * - Authorization
     * - x-ms-date
     * - x-ms-content-sha256
     * 
     * @param request to be send
     * @param credential to use
     * @param secret to use
     */
    private static Map<String, String> generateHeader(HttpUriRequest request, String credential, String secret)
            throws URISyntaxException, IOException, NoSuchAlgorithmException {
        String requestTime = formatter.format(ZonedDateTime.now(ZoneOffset.UTC));

        String contentHash = buildContentHash(request);
        // SignedHeaders
        String signedHeaders = "x-ms-date;host;x-ms-content-sha256";

        // Signature
        String methodName = request.getRequestLine().getMethod().toUpperCase();
        URIBuilder uri = new URIBuilder(request.getRequestLine().getUri());
        String scheme = uri.getScheme() + "://";
        String requestPath = uri.toString().substring(scheme.length()).substring(uri.getHost().length());
        String host = new URIBuilder(request.getRequestLine().getUri()).getHost();
        String toSign = String.format("%s\n%s\n%s;%s;%s", methodName, requestPath, requestTime, host, contentHash);

        byte[] decodedKey = Base64.getDecoder().decode(secret);
        String signature = Base64.getEncoder()
                .encodeToString(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, decodedKey).hmac(toSign));

        // Compose headers
        Map<String, String> headers = new HashMap<>();
        headers.put("x-ms-date", requestTime);
        headers.put("x-ms-content-sha256", contentHash);

        String authorization = String.format("HMAC-SHA256 Credential=%s, SignedHeaders=%s, Signature=%s", credential,
                signedHeaders, signature);
        headers.put("Authorization", authorization);

        return headers;
    }

    private static String buildContentHash(HttpUriRequest request) throws IOException, NoSuchAlgorithmException {
        String content = "";
        if (request instanceof HttpEntityEnclosingRequest) {
            try {
                content = new BufferedReader(
                        new InputStreamReader(((HttpEntityEnclosingRequest) request).getEntity().getContent(),
                                StandardCharsets.UTF_8))
                                        .lines()
                                        .collect(Collectors.joining("\n"));
            } finally {
                ((HttpEntityEnclosingRequest) request).getEntity().getContent().close();
            }
        }

        MessageDigest SHA_256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = new DigestUtils(SHA_256).digest(content);
        return Base64.getEncoder().encodeToString(digest);
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
