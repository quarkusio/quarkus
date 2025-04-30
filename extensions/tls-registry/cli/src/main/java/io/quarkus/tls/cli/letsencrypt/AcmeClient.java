package io.quarkus.tls.cli.letsencrypt;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    static System.Logger LOGGER = System.getLogger("lets-encrypt-acme-client");

    private static final String TOKEN_REGEX = "[A-Za-z0-9_-]+";

    private final String challengeUrl;
    private final String certsUrl;
    private final WebClientOptions options;
    private final Vertx vertx;

    final String managementUser;
    final String managementPassword;
    final String managementKey;

    private final WebClient managementClient;

    public AcmeClient(String managementUrl,
            String managementUser,
            String managementPassword,
            String managementKey) {
        this.vertx = Vertx.vertx();
        LOGGER.log(INFO, "\uD83D\uDD35 Creating AcmeClient with {0}", managementUrl);

        // It will need to become configurable to support mTLS, etc
        options = new WebClientOptions();
        if (managementUrl.startsWith("https://")) {
            options.setSsl(true).setTrustAll(true).setVerifyHost(false);
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

    public boolean checkReadiness() {

        // Check status
        LOGGER.log(INFO, "\uD83D\uDD35 Checking management challenge endpoint status using {0}", challengeUrl);
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
                    LOGGER.log(ERROR,
                            "⚠\uFE0F Let's Encrypt challenge endpoint is not found, make sure that the build-time property `quarkus.tls.lets-encrypt.enabled` is set to `true`");
                    return false;
                }
                default -> {
                    LOGGER.log(WARNING, "⚠\uFE0F Unexpected status code from the management challenge endpoint: " + status);
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.log(DEBUG, "Failed to check the management challenge endpoint status", e);
            LOGGER.log(ERROR,
                    "⚠\uFE0F Quarkus management endpoint is not ready, make sure the Quarkus application is running.");
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
                LOGGER.log(DEBUG, "HTTP 01 challenge is selected");
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
            throw new RuntimeException("Invalid certificate authority challenge");
        }

        LOGGER.log(DEBUG, "Preparing a selected challenge content for token {0}", token);
        String selectedChallengeString = selectedChallenge.getKeyAuthorization(account);

        // respond to the http challenge
        if (managementClient != null) {
            //TODO: Use JsonObject once POST is supported
            //JsonObject challenge = new JsonObject().put("challenge-resource", token).put("challenge-content",
            //        selectedChallengeString);
            HttpRequest<Buffer> request = managementClient.getAbs(challengeUrl);
            request.addQueryParam("challenge-resource", token).addQueryParam("challenge-content", selectedChallengeString);
            addKeyAndUser(request);
            LOGGER.log(DEBUG, "Sending token {0} and challenge content to the management challenge endpoint", token,
                    selectedChallengeString);

            HttpResponse<Buffer> response = await(request.send());

            if (response.statusCode() != 204) {
                LOGGER.log(ERROR,
                        "⚠\uFE0F Failed to upload challenge content to the management challenge endpoint, status code: "
                                + response.statusCode());
                throw new RuntimeException("Failed to respond to certificate authority challenge");
            } else {
                LOGGER.log(INFO, "\uD83D\uDD35 Challenge ready for token {0}, waiting for Let's Encrypt to validate...", token);
            }
        }
        return selectedChallenge;
    }

    @Override
    public void cleanupAfterChallenge(AcmeAccount account, AcmeChallenge challenge) throws AcmeException {
        LOGGER.log(INFO, "\uD83D\uDD35 Performing cleanup after the challenge");

        Assert.checkNotNullParam("account", account);
        Assert.checkNotNullParam("challenge", challenge);
        // ensure the token is valid before proceeding
        String token = challenge.getToken();
        if (!token.matches(TOKEN_REGEX)) {
            throw new RuntimeException("Invalid certificate authority challenge");
        }

        LOGGER.log(DEBUG, "Requesting the management challenge endpoint to delete a challenge resource {0}", token);

        HttpRequest<Buffer> request = managementClient.deleteAbs(challengeUrl);
        addKeyAndUser(request);
        HttpResponse<Buffer> response = await(request.send());
        if (response.statusCode() != 204) {
            throw new RuntimeException("Failed to clear challenge content in the Quarkus management endpoint");
        }
    }

    public void certificateChainAndKeyAreReady() {
        LOGGER.log(INFO,
                "\uD83D\uDD35 Notifying management challenge endpoint that a new certificate chain and private key are ready");
        HttpRequest<Buffer> request = managementClient.postAbs(certsUrl);
        addKeyAndUser(request);
        HttpResponse<Buffer> response = await(request.send());
        if (response.statusCode() != 204) {
            throw new RuntimeException("Failed to notify the Quarkus management endpoint");
        }
    }

    private void addKeyAndUser(HttpRequest<Buffer> request) {
        if (managementKey != null) {
            request.addQueryParam("key", managementKey);
        }
        if (managementUser != null && managementPassword != null) {
            request.basicAuthentication(managementUser, managementPassword);
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
