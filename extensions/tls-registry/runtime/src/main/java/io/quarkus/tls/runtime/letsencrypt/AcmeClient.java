package io.quarkus.tls.runtime.letsencrypt;

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A non-blocking ACME client (RFC 8555) for the HTTP-01 challenge, with ACME Renewal Information (RFC 9773).
 * <p>
 * The client keeps the replay nonce of the last response and retries a request once when the server rejects
 * the nonce. It is not thread-safe: the caller sends one request at a time.
 */
final class AcmeClient {

    private static final Logger LOGGER = Logger.getLogger(AcmeClient.class);

    static final String CONTENT_TYPE_JOSE = "application/jose+json";
    static final String CONTENT_TYPE_PEM_CHAIN = "application/pem-certificate-chain";
    private static final String USER_AGENT = "quarkus-lets-encrypt-client";
    private static final String BAD_NONCE = "urn:ietf:params:acme:error:badNonce";
    private static final Duration DEFAULT_POLL_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_POLL_DELAY = Duration.ofSeconds(30);

    private final Vertx vertx;
    private final HttpClient client;
    private final String directoryUrl;
    private final Jws jws;
    private final Duration pollTimeout;

    private JsonObject directory;
    private String nonce;
    private String accountUrl;

    /**
     * Create a client.
     *
     * @param vertx the Vert.x instance, used for the poll timers
     * @param client the HTTP client to reach the ACME server
     * @param directoryUrl the URL of the ACME directory
     * @param jws the signer of the account key
     * @param pollTimeout how long to wait for an authorization or an order to leave the pending states
     */
    AcmeClient(Vertx vertx, HttpClient client, String directoryUrl, Jws jws, Duration pollTimeout) {
        this.vertx = vertx;
        this.client = client;
        this.directoryUrl = directoryUrl;
        this.jws = jws;
        this.pollTimeout = pollTimeout;
    }

    /**
     * An ACME error: the problem document of the server (RFC 7807), or a protocol error of the client.
     */
    static final class AcmeException extends RuntimeException {

        private final String type;

        AcmeException(String message, String type) {
            super(message);
            this.type = type;
        }

        /**
         * Get the problem type.
         *
         * @return the problem type URN, or {@code null} for an error of the client
         */
        String type() {
            return type;
        }
    }

    /**
     * A response of the ACME server.
     *
     * @param status the HTTP status
     * @param headers the response headers
     * @param body the response body
     */
    record Response(int status, MultiMap headers, Buffer body) {

        JsonObject json() {
            return body.length() == 0 ? new JsonObject() : body.toJsonObject();
        }

        String location() {
            return headers.get("Location");
        }
    }

    /**
     * The suggested renewal window of a certificate (RFC 9773, section 4.2).
     *
     * @param start the start of the window
     * @param end the end of the window
     * @param retryAfter how long to wait before the next query, if the server says it
     */
    record RenewalWindow(Instant start, Instant end, Optional<Duration> retryAfter) {
    }

    /**
     * Get the account key signer.
     *
     * @return the signer
     */
    Jws jws() {
        return jws;
    }

    /**
     * Get the ACME directory, and read it on the first call.
     *
     * @return the directory
     */
    Future<JsonObject> directory() {
        if (directory != null) {
            return Future.succeededFuture(directory);
        }
        return send(HttpMethod.GET, directoryUrl, null, null).map(response -> {
            expectStatus(response, 200);
            directory = response.json();
            return directory;
        });
    }

    /**
     * Get the profiles that the server announces in its directory.
     *
     * @return the profile names
     */
    Future<List<String>> profiles() {
        return directory().map(d -> {
            JsonObject meta = d.getJsonObject("meta", new JsonObject());
            return List.copyOf(meta.getJsonObject("profiles", new JsonObject()).fieldNames());
        });
    }

    /**
     * Find or create the account of the key, and agree to the terms of service.
     *
     * @param contacts the contact URLs, for example {@code mailto:admin@example.org}
     * @return the account URL
     */
    Future<String> account(List<String> contacts) {
        JsonObject payload = new JsonObject().put("termsOfServiceAgreed", true);
        if (!contacts.isEmpty()) {
            payload.put("contact", new JsonArray(new ArrayList<>(contacts)));
        }
        return resource("newAccount")
                .compose(url -> signedPost(url, payload, true))
                .map(response -> {
                    expectStatus(response, 200, 201);
                    accountUrl = response.location();
                    if (accountUrl == null) {
                        throw new AcmeException("The ACME server returned no account URL", null);
                    }
                    return accountUrl;
                });
    }

    /**
     * Create an order for the given domains.
     *
     * @param domains the domains
     * @param profile the ACME profile, if any
     * @param replaces the renewal certificate identifier of the certificate that the new one replaces, if any
     * @return the order URL and the order
     */
    Future<Response> newOrder(List<String> domains, Optional<String> profile, Optional<String> replaces) {
        JsonArray identifiers = new JsonArray();
        for (String domain : domains) {
            identifiers.add(new JsonObject().put("type", "dns").put("value", domain));
        }
        JsonObject payload = new JsonObject().put("identifiers", identifiers);
        profile.ifPresent(p -> payload.put("profile", p));
        replaces.ifPresent(r -> payload.put("replaces", r));
        return resource("newOrder")
                .compose(url -> signedPost(url, payload, false))
                .map(response -> {
                    expectStatus(response, 201);
                    return response;
                });
    }

    /**
     * Answer the HTTP-01 challenge of an authorization and wait until the server validates it.
     *
     * @param authorizationUrl the authorization URL
     * @param publisher publishes and removes the key authorization of a token
     * @return a future completed when the authorization is valid
     */
    Future<Void> authorize(String authorizationUrl, ChallengePublisher publisher) {
        return postAsGet(authorizationUrl).compose(response -> {
            JsonObject authorization = response.json();
            String status = authorization.getString("status");
            if ("valid".equals(status)) {
                return Future.succeededFuture();
            }
            if (!"pending".equals(status)) {
                return Future.failedFuture(new AcmeException("The authorization " + authorizationUrl + " is " + status,
                        null));
            }
            JsonObject challenge = null;
            for (Object c : authorization.getJsonArray("challenges", new JsonArray())) {
                if (c instanceof JsonObject candidate && "http-01".equals(candidate.getString("type"))) {
                    challenge = candidate;
                }
            }
            if (challenge == null) {
                return Future.failedFuture(new AcmeException(
                        "The ACME server offers no http-01 challenge for " + identifier(authorization), null));
            }
            String token = challenge.getString("token");
            String challengeUrl = challenge.getString("url");
            publisher.publish(token, token + "." + jws.thumbprint());
            return signedPost(challengeUrl, new JsonObject(), false)
                    .compose(r -> {
                        expectStatus(r, 200);
                        return poll(authorizationUrl, "valid", List.of("pending", "processing"),
                                deadline());
                    })
                    .<Void> mapEmpty()
                    .eventually(() -> {
                        publisher.remove(token);
                        return Future.succeededFuture();
                    });
        });
    }

    /**
     * Send the certificate signing request of an order and wait until the certificate is ready.
     *
     * @param orderUrl the order URL
     * @param order the order
     * @param csr the DER encoding of the certificate signing request
     * @return the URL of the certificate chain
     */
    Future<String> finalizeOrder(String orderUrl, JsonObject order, byte[] csr) {
        JsonObject payload = new JsonObject().put("csr", Base64.getUrlEncoder().withoutPadding().encodeToString(csr));
        return signedPost(order.getString("finalize"), payload, false)
                .compose(response -> {
                    expectStatus(response, 200);
                    return poll(orderUrl, "valid", List.of("pending", "ready", "processing"), deadline());
                })
                .map(valid -> {
                    String certificate = valid.getString("certificate");
                    if (certificate == null) {
                        throw new AcmeException("The valid order " + orderUrl + " has no certificate URL", null);
                    }
                    return certificate;
                });
    }

    /**
     * Download a certificate chain.
     *
     * @param certificateUrl the certificate URL
     * @return the PEM chain, the end-entity certificate first
     */
    Future<String> downloadCertificate(String certificateUrl) {
        return signedPost(certificateUrl, null, false, CONTENT_TYPE_PEM_CHAIN).map(response -> {
            expectStatus(response, 200);
            return response.body().toString();
        });
    }

    /**
     * Get the suggested renewal window of a certificate.
     *
     * @param certificateId the renewal certificate identifier
     * @return the window, or an empty optional if the server does not support ACME Renewal Information
     */
    Future<Optional<RenewalWindow>> renewalWindow(String certificateId) {
        return directory().compose(d -> {
            String base = d.getString("renewalInfo");
            if (base == null) {
                return Future.succeededFuture(Optional.empty());
            }
            String url = base.endsWith("/") ? base + certificateId : base + "/" + certificateId;
            return send(HttpMethod.GET, url, null, null).map(response -> {
                expectStatus(response, 200);
                JsonObject window = response.json().getJsonObject("suggestedWindow");
                if (window == null) {
                    throw new AcmeException("The renewal information of " + certificateId + " has no window", null);
                }
                return Optional.of(new RenewalWindow(instant(window.getString("start")),
                        instant(window.getString("end")), retryAfter(response)));
            });
        });
    }

    /**
     * Send a POST-as-GET request.
     *
     * @param url the resource URL
     * @return the response
     */
    Future<Response> postAsGet(String url) {
        return signedPost(url, null, false);
    }

    private Future<Response> signedPost(String url, JsonObject payload, boolean withJwk) {
        return signedPost(url, payload, withJwk, null);
    }

    private Future<Response> signedPost(String url, JsonObject payload, boolean withJwk, String accept) {
        return signedPost(url, payload, withJwk, accept, true);
    }

    private Future<Response> signedPost(String url, JsonObject payload, boolean withJwk, String accept,
            boolean retryBadNonce) {
        return nonce().compose(n -> {
            JsonObject body;
            try {
                body = withJwk ? jws.signWithJwk(url, n, payload) : jws.signWithKid(url, n, accountUrl(), payload);
            } catch (GeneralSecurityException e) {
                return Future.failedFuture(e);
            }
            nonce = null;
            return send(HttpMethod.POST, url, body.toBuffer(), accept);
        }).compose(response -> {
            if (retryBadNonce && response.status() == 400 && BAD_NONCE.equals(response.json().getString("type"))) {
                LOGGER.debugf("The ACME server rejected the nonce of %s, retrying once", url);
                return signedPost(url, payload, withJwk, accept, false);
            }
            if (response.status() >= 400) {
                return Future.failedFuture(problem(url, response));
            }
            return Future.succeededFuture(response);
        });
    }

    private String accountUrl() {
        if (accountUrl == null) {
            throw new AcmeException("The ACME account is not known yet", null);
        }
        return accountUrl;
    }

    private Future<String> nonce() {
        if (nonce != null) {
            return Future.succeededFuture(nonce);
        }
        return resource("newNonce")
                .compose(url -> send(HttpMethod.HEAD, url, null, null))
                .map(response -> {
                    if (nonce == null) {
                        throw new AcmeException("The ACME server returned no replay nonce", null);
                    }
                    return nonce;
                });
    }

    private Future<String> resource(String name) {
        return directory().map(d -> {
            String url = d.getString(name);
            if (url == null) {
                throw new AcmeException("The ACME directory has no " + name + " resource", null);
            }
            return url;
        });
    }

    private Future<Response> send(HttpMethod method, String url, Buffer body, String accept) {
        RequestOptions options = new RequestOptions().setMethod(method).setAbsoluteURI(url)
                .setSsl(url.regionMatches(true, 0, "https://", 0, 8))
                // RFC 8555, section 6.1: every request has a User-Agent header
                .putHeader("User-Agent", USER_AGENT);
        if (body != null) {
            options.putHeader("Content-Type", CONTENT_TYPE_JOSE);
        }
        if (accept != null) {
            options.putHeader("Accept", accept);
        }
        return client.request(options)
                .compose(request -> body == null ? request.send() : request.send(body))
                .compose(response -> response.body().map(b -> toResponse(response, b)));
    }

    private Response toResponse(HttpClientResponse response, Buffer body) {
        String replayNonce = response.getHeader("Replay-Nonce");
        if (replayNonce != null) {
            nonce = replayNonce;
        }
        return new Response(response.statusCode(), response.headers(), body);
    }

    private Future<JsonObject> poll(String url, String expected, List<String> waiting, Instant deadline) {
        return postAsGet(url).compose(response -> {
            JsonObject resource = response.json();
            String status = resource.getString("status");
            if (expected.equals(status)) {
                return Future.succeededFuture(resource);
            }
            if (!waiting.contains(status)) {
                String error = resource.getJsonObject("error") != null
                        ? ": " + resource.getJsonObject("error").getString("detail")
                        : "";
                for (Object c : resource.getJsonArray("challenges", new JsonArray())) {
                    if (c instanceof JsonObject challenge && challenge.getJsonObject("error") != null) {
                        error = ": " + challenge.getJsonObject("error").getString("detail");
                    }
                }
                return Future.failedFuture(new AcmeException(url + " is " + status + error, null));
            }
            if (Instant.now().isAfter(deadline)) {
                return Future.failedFuture(new AcmeException(url + " is still " + status + " after " + pollTimeout,
                        null));
            }
            Duration delay = retryAfter(response).orElse(DEFAULT_POLL_DELAY);
            if (delay.compareTo(MAX_POLL_DELAY) > 0) {
                delay = MAX_POLL_DELAY;
            }
            Promise<JsonObject> promise = Promise.promise();
            vertx.setTimer(Math.max(1, delay.toMillis()),
                    id -> poll(url, expected, waiting, deadline).onComplete(promise));
            return promise.future();
        });
    }

    private Instant deadline() {
        return Instant.now().plus(pollTimeout);
    }

    private static void expectStatus(Response response, int... expected) {
        for (int status : expected) {
            if (response.status() == status) {
                return;
            }
        }
        String body = response.body().toString();
        throw new AcmeException("Unexpected status " + response.status() + " from the ACME server"
                + (body.isBlank() ? "" : ": " + body.substring(0, Math.min(body.length(), 300)).strip()), null);
    }

    private static AcmeException problem(String url, Response response) {
        JsonObject problem;
        try {
            problem = response.json();
        } catch (RuntimeException e) {
            problem = new JsonObject();
        }
        String detail = problem.getString("detail", "status " + response.status());
        return new AcmeException("The ACME request to " + url + " failed: " + detail, problem.getString("type"));
    }

    private static String identifier(JsonObject authorization) {
        JsonObject identifier = authorization.getJsonObject("identifier", new JsonObject());
        return identifier.getString("value", "an unknown identifier");
    }

    static Optional<Duration> retryAfter(Response response) {
        String value = response.headers().get("Retry-After");
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.ofSeconds(Long.parseLong(value.trim())));
        } catch (NumberFormatException e) {
            try {
                Instant date = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                Duration delay = Duration.between(Instant.now(), date);
                return Optional.of(delay.isNegative() ? Duration.ZERO : delay);
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
    }

    private static Instant instant(String value) {
        if (value == null) {
            throw new AcmeException("The renewal window has no start or end", null);
        }
        return Instant.parse(value);
    }

    /**
     * Publishes the key authorization of an HTTP-01 challenge so that the application serves it at
     * {@code /.well-known/acme-challenge/<token>}.
     */
    interface ChallengePublisher {

        /**
         * Serve the key authorization for the token.
         *
         * @param token the token
         * @param keyAuthorization the key authorization
         */
        void publish(String token, String keyAuthorization);

        /**
         * Stop serving the token.
         *
         * @param token the token
         */
        void remove(String token);
    }
}
