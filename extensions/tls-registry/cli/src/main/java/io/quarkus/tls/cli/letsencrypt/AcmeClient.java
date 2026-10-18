package io.quarkus.tls.cli.letsencrypt;

import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.AUDIT;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;
import org.wildfly.security.x500.cert.acme.AcmeAccount;
import org.wildfly.security.x500.cert.acme.AcmeChallenge;
import org.wildfly.security.x500.cert.acme.AcmeClientSpi;
import org.wildfly.security.x500.cert.acme.AcmeException;

import io.smallrye.common.constraint.Assert;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class AcmeClient extends AcmeClientSpi {

    static Logger LOGGER = Logger.getLogger(AcmeClient.class);

    private static final String TOKEN_REGEX = "[A-Za-z0-9_-]+";

    // Rate limiting: max 10 requests per minute to prevent DoS and Let's Encrypt rate limit exhaustion
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute

    private final String challengeUrl;
    private final String certsUrl;
    private final WebClientOptions options;
    private final Vertx vertx;

    final String managementUser;
    final String managementPassword;
    final String managementKey;

    private final WebClient managementClient;

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());

    public AcmeClient(String managementUrl,
            String managementUser,
            String managementPassword,
            String managementKey,
            boolean insecureMode) {
        this.vertx = Vertx.vertx();
        LOGGER.infof("\uD83D\uDD35 Creating AcmeClient with %s", managementUrl);

        // It will need to become configurable to support mTLS, etc
        options = new WebClientOptions();
        if (managementUrl.startsWith("https://")) {
            options.setSsl(true);

            // Only disable SSL validation if explicitly requested for development/testing
            if (insecureMode) {
                AUDIT.error("SSL certificate validation DISABLED for management endpoint: " + managementUrl);
                AUDIT.error("This configuration is INSECURE and must not be used in production");
                LOGGER.warn("⚠️  WARNING: SSL certificate validation is DISABLED");
                LOGGER.warn("⚠️  This is INSECURE and should only be used for development/testing");
                LOGGER.warn("⚠️  NEVER use --insecure flag in production environments");
                options.setTrustAll(true).setVerifyHost(false);
            }
            // Otherwise, use system trust store for proper SSL validation (secure default)
        }
        this.managementClient = WebClient.create(vertx, options);
        if (managementUrl.endsWith("/q/lets-encrypt")) {
            this.challengeUrl = managementUrl + "/challenge";
            this.certsUrl = managementUrl + "/certs";
        } else {
            this.challengeUrl = managementUrl + "/q/lets-encrypt/challenge";
            this.certsUrl = managementUrl + "/q/lets-encrypt/certs";
        }
        this.managementUser = managementUser;
        this.managementPassword = managementPassword;
        this.managementKey = managementKey;
    }

    private void checkRateLimit(String operation) {
        long now = System.currentTimeMillis();
        long windowStart = windowStartTime.get();

        if (now - windowStart >= RATE_LIMIT_WINDOW_MS) {
            if (windowStartTime.compareAndSet(windowStart, now)) {
                requestCount.set(0);
            }
        }

        int currentCount = requestCount.incrementAndGet();
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            AUDIT.warn("Rate limit exceeded - operation: " + operation + ", requests: " + currentCount + "/"
                    + MAX_REQUESTS_PER_MINUTE + ", endpoint: " + challengeUrl);
            LOGGER.warn("⚠️  Rate limit exceeded: " + currentCount + " requests in the last minute");
            LOGGER.warn("⚠️  Maximum allowed: " + MAX_REQUESTS_PER_MINUTE + " requests per minute");
            throw new RuntimeException(
                    "Rate limit exceeded: too many ACME challenge requests. Wait 60 seconds and try again.");
        }
    }

    public boolean checkReadiness() {

        // Check status
        LOGGER.infof("\uD83D\uDD35 Checking management challenge endpoint status using %s", challengeUrl);
        HttpRequest<Buffer> request = managementClient.getAbs(challengeUrl);
        addKeyAndUser(request);
        try {
            HttpResponse<Buffer> response = await(request.send());
            int status = response.statusCode();
            switch (status) {
                case 200, 204 -> {
                    return true;
                }
                case 404 -> {
                    LOGGER.error(
                            "⚠️ Let's Encrypt challenge endpoint is not found, make sure that the build-time property `quarkus.tls.lets-encrypt.enabled` is set to `true`");
                    return false;
                }
                default -> {
                    LOGGER.warn("⚠️ Unexpected status code from the management challenge endpoint: " + status);
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to check the management challenge endpoint status", e);
            LOGGER.error("⚠️ Quarkus management endpoint is not ready, make sure the Quarkus application is running.");
            return false;
        }

    }

    @Override
    public AcmeChallenge proveIdentifierControl(AcmeAccount account, List<AcmeChallenge> challenges)
            throws AcmeException {
        Assert.checkNotNullParam("account", account);
        Assert.checkNotNullParam("challenges", challenges);
        AcmeChallenge selectedChallenge = null;
        for (AcmeChallenge challenge : challenges) {
            if (challenge.getType() == AcmeChallenge.Type.HTTP_01) {
                AUDIT.info("Selected HTTP-01 challenge for domain validation");
                LOGGER.debug("HTTP 01 challenge is selected");
                selectedChallenge = challenge;
                break;
            }
        }
        if (selectedChallenge == null) {
            throw new RuntimeException("Missing certificate authority challenge");
        }

        // ensure the token is valid before proceeding
        String token = selectedChallenge.getToken();
        if (!token.matches(TOKEN_REGEX)) {
            AUDIT.error("Invalid challenge token format - rejecting");
            throw new RuntimeException("Invalid certificate authority challenge");
        }

        LOGGER.debugf("Preparing a selected challenge content for token %s", token);
        String selectedChallengeString = selectedChallenge.getKeyAuthorization(account);

        // Check rate limit before uploading challenge
        checkRateLimit("challenge-upload");

        // respond to the http challenge
        if (managementClient != null) {
            //TODO: Use JsonObject once POST is supported
            //JsonObject challenge = new JsonObject().put("challenge-resource", token).put("challenge-content",
            //        selectedChallengeString);
            HttpRequest<Buffer> request = managementClient.getAbs(challengeUrl);
            request.addQueryParam("challenge-resource", token).addQueryParam("challenge-content", selectedChallengeString);
            addKeyAndUser(request);
            AUDIT.info("Uploading challenge to management endpoint - token: " + token.substring(0, Math.min(8, token.length()))
                    + "..., endpoint: " + challengeUrl);
            LOGGER.debugf("Sending token %s and challenge content to the management challenge endpoint", token,
                    selectedChallengeString);

            HttpResponse<Buffer> response = await(request.send());

            if (response.statusCode() != 204) {
                AUDIT.error("Failed to upload challenge - status: " + response.statusCode() + ", endpoint: " + challengeUrl);
                LOGGER.error("⚠️ Failed to upload challenge content to the management challenge endpoint, status code: "
                        + response.statusCode());
                throw new RuntimeException("Failed to respond to certificate authority challenge");
            } else {
                LOGGER.infof("\uD83D\uDD35 Challenge ready for token %s, waiting for Let's Encrypt to validate...", token);
            }
        }
        return selectedChallenge;
    }

    @Override
    public void cleanupAfterChallenge(AcmeAccount account, AcmeChallenge challenge) throws AcmeException {
        LOGGER.info("\uD83D\uDD35 Performing cleanup after the challenge");

        Assert.checkNotNullParam("account", account);
        Assert.checkNotNullParam("challenge", challenge);
        // ensure the token is valid before proceeding
        String token = challenge.getToken();
        if (!token.matches(TOKEN_REGEX)) {
            throw new RuntimeException("Invalid certificate authority challenge");
        }

        LOGGER.debugf("Requesting the management challenge endpoint to delete a challenge resource %s", token);

        // Check rate limit before cleanup
        checkRateLimit("challenge-cleanup");

        HttpRequest<Buffer> request = managementClient.deleteAbs(challengeUrl);
        addKeyAndUser(request);
        HttpResponse<Buffer> response = await(request.send());
        if (response.statusCode() != 204) {
            throw new RuntimeException("Failed to clear challenge content in the Quarkus management endpoint");
        }
    }

    public void certificateChainAndKeyAreReady() {
        LOGGER.info(
                "\uD83D\uDD35 Notifying management challenge endpoint that a new certificate chain and private key are ready");

        // Check rate limit before notification
        checkRateLimit("certificate-notification");

        HttpRequest<Buffer> request = managementClient.postAbs(certsUrl);
        addKeyAndUser(request);
        HttpResponse<Buffer> response = await(request.send());
        if (response.statusCode() != 204) {
            throw new RuntimeException("Failed to notify the Quarkus management endpoint");
        }
    }

    private void addKeyAndUser(HttpRequest<Buffer> request) {
        if (managementKey != null) {
            AUDIT.info("Using API key authentication for management endpoint");
            request.addQueryParam("key", managementKey);
        } else if (managementUser != null && managementPassword != null) {
            AUDIT.info("Using basic authentication for management endpoint (user: " + managementUser + ")");
            request.basicAuthentication(managementUser, managementPassword);
        } else {
            AUDIT.warn("No authentication credentials provided for management endpoint");
        }
    }

    private <T> T await(Future<T> future) {
        try {
            return future.toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
