package io.quarkus.analytics.rest;

import static io.quarkus.analytics.util.PropertyUtils.getProperty;
import static io.quarkus.analytics.util.StringUtils.getObjectMapper;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.quarkus.analytics.dto.config.AnalyticsRemoteConfig;
import io.quarkus.analytics.dto.config.Identity;
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.dto.segment.Track;
import io.quarkus.devtools.messagewriter.MessageWriter;

/**
 * Client to post the analytics data to the upstream collection tool.
 * We use plain REST API calls and not any wrapping library.
 */
public class RestClient implements ConfigClient, SegmentClient {

    public static final int DEFAULT_TIMEOUT = 3000;// milliseconds
    static final String IDENTITY_ENDPOINT = "v1/identify";
    static final String TRACK_ENDPOINT = "v1/track";
    static final URI CONFIG_URI = getUri("https://quarkus.io/assets/json/03656937-19FD-4C83-9066-C76631D445EA.json");
    private static final String AUTH_HEADER = getAuthHeader("WdCBreXheGC541sGjMMvUknY8c6lLxy5");
    private static final int SEGMENT_POST_RESPONSE_CODE = 200; // sad but true

    static URI getUri(final String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * All with the same authentication
     *
     * @return BAsic Auth Header value
     * @param key
     */
    static String getAuthHeader(final String key) {
        final String auth = key + ":";
        return "Basic " + Base64.getEncoder().encodeToString(
                auth.getBytes(StandardCharsets.ISO_8859_1));
    }

    private final MessageWriter log;

    private final URI segmentIdentityUri;

    private final URI segmentTraceUri;

    private final int timeoutMs = getProperty("quarkus.analytics.timeout", DEFAULT_TIMEOUT);

    public RestClient(MessageWriter log) {
        this.log = log;
        final String segmentBaseUri = getProperty("quarkus.analytics.uri.base", "https://api.segment.io/");
        this.segmentIdentityUri = getUri(segmentBaseUri + IDENTITY_ENDPOINT);
        this.segmentTraceUri = getUri(segmentBaseUri + TRACK_ENDPOINT);
    }

    public RestClient() {
        this(MessageWriter.info());
    }

    @Override
    public CompletableFuture<HttpResponse<String>> postIdentity(final Identity identity) {
        return post(identity, segmentIdentityUri);
    }

    @Override
    public CompletableFuture<HttpResponse<String>> postTrack(Track track) {
        return post(track, segmentTraceUri);
    }

    @Override
    public Optional<AnalyticsRemoteConfig> getConfig() {
        return getConfig(CONFIG_URI);
    }

    Optional<AnalyticsRemoteConfig> getConfig(final URI uri) {
        try {
            final HttpClient httpClient = createHttpClient();

            final HttpRequest request = createRequest(uri)
                    .GET()
                    .build();

            final CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            final HttpResponse<String> response = responseFuture.get(timeoutMs, MILLISECONDS);
            final int statusCode = response.statusCode();

            if (statusCode == SEGMENT_POST_RESPONSE_CODE) {
                final String body = response.body();
                return Optional.of(getObjectMapper().readValue(body, RemoteConfig.class));
            }
            return Optional.empty();
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Analytics remote config not received. " +
                        e.getClass().getName() + ": " +
                        (e.getMessage() == null ? "(no message)" : e.getMessage()));
            }
        }
        return Optional.empty();
    }

    CompletableFuture<HttpResponse<String>> post(final Serializable payload, final URI url) {
        try {
            final HttpClient httpClient = createHttpClient();

            final String toSend = getObjectMapper().writeValueAsString(payload);
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Analytics to send: " + toSend);
            }
            final HttpRequest request = createRequest(url)
                    .POST(HttpRequest.BodyPublishers.ofString(toSend))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            log.warn("[Quarkus build analytics] Analytics not sent. " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private HttpRequest.Builder createRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("authorization", AUTH_HEADER)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                // the JDK client does not close the connection
                .timeout(Duration.ofMillis(timeoutMs));
    }
}
