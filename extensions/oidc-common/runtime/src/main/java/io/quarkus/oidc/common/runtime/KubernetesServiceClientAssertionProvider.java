package io.quarkus.oidc.common.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Jwt.Source;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

final class KubernetesServiceClientAssertionProvider implements ClientAssertionProvider, Closeable {

    private record ClientAssertion(String bearerToken, long expiresAt, long timerId) {
        private boolean isExpired() {
            final long nowSecs = System.currentTimeMillis() / 1000;
            return nowSecs > expiresAt;
        }
    }

    private static final Logger LOG = Logger.getLogger(KubernetesServiceClientAssertionProvider.class);
    private static final String SPIFFE_ID_SCHEME = "spiffe://";
    private final Vertx vertx;
    private final Path tokenPath;
    private final String clientAssertionType;
    private final String tokenType;
    private volatile ClientAssertion clientAssertion;

    KubernetesServiceClientAssertionProvider(Vertx vertx, Path tokenPath, Source source) {
        this.vertx = vertx;
        this.tokenPath = tokenPath;
        if (source == Source.BEARER) {
            this.clientAssertionType = OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE;
            this.tokenType = "JWT bearer";
        } else if (source == Source.SPIFFE) {
            this.clientAssertionType = OidcConstants.SPIFFE_SVID_CLIENT_ASSERTION_TYPE;
            this.tokenType = "SPIFFE JWT-SVID";
        } else {
            throw new IllegalStateException("Unsupported JWT source: " + source);
        }
        this.clientAssertion = loadFromFileSystem();
    }

    @Override
    public String getClientAssertion() {
        ClientAssertion clientAssertion = this.clientAssertion;
        if (isInvalid(clientAssertion)) {
            clientAssertion = loadClientAssertion();
        }
        return clientAssertion == null ? null : clientAssertion.bearerToken;
    }

    @Override
    public String getClientAssertionType() {
        return clientAssertionType;
    }

    @Override
    public void close() {
        cancelRefresh();
        clientAssertion = null;
    }

    private synchronized ClientAssertion loadClientAssertion() {
        if (isInvalid(clientAssertion)) {
            cancelRefresh();
            clientAssertion = loadFromFileSystem();
        }
        return clientAssertion;
    }

    private long scheduleRefresh(long expiresAt) {
        // in K8 and OCP, tokens are proactively rotated at 80 % of their TTL
        long delay = (long) (expiresAt * 0.85);
        return vertx.setTimer(delay, new Handler<Long>() {
            @Override
            public void handle(Long ignored) {
                KubernetesServiceClientAssertionProvider.this.clientAssertion = loadFromFileSystem();
            }
        });
    }

    private void cancelRefresh() {
        if (clientAssertion != null) {
            vertx.cancelTimer(clientAssertion.timerId);
        }
    }

    private ClientAssertion loadFromFileSystem() {
        if (Files.exists(tokenPath)) {
            try {
                String bearerToken = Files.readString(tokenPath).trim();
                if (bearerToken.isEmpty()) {
                    LOG.errorf("%s token file at path %s is empty or contains only whitespace",
                            tokenType, tokenPath);
                    return null;
                }
                JsonObject claims = OidcCommonUtils.decodeJwtContent(bearerToken);
                if (OidcConstants.SPIFFE_SVID_CLIENT_ASSERTION_TYPE.equals(clientAssertionType)
                        && !verifySpiffeIdSubClaim(claims)) {
                    LOG.error(
                            "SPIFFE JWT-SVID token 'sub' claim is missing or does not start with '" + SPIFFE_ID_SCHEME + "'");
                    return null;
                }
                Long expiresAt = getExpiresAtFromExpClaim(claims);
                if (expiresAt != null) {
                    return new ClientAssertion(bearerToken, expiresAt, scheduleRefresh(expiresAt));
                } else {
                    LOG.errorf("%s token or its expiry claim is invalid", tokenType);
                }
            } catch (IOException e) {
                LOG.errorf(e, "Failed to read file with a %s token at path: %s", tokenType, tokenPath);
            }
        } else {
            LOG.warnf("Cannot find a file with a %s token at path: %s", tokenType, tokenPath);
        }
        return null;
    }

    private static boolean isInvalid(ClientAssertion clientAssertion) {
        return clientAssertion == null || clientAssertion.isExpired();
    }

    private static boolean verifySpiffeIdSubClaim(JsonObject claims) {
        String sub = claims != null ? claims.getString(Claims.sub.name()) : null;
        return (sub == null || !sub.startsWith(SPIFFE_ID_SCHEME)) ? false : true;

    }

    private Long getExpiresAtFromExpClaim(JsonObject claims) {
        if (claims == null || !claims.containsKey(Claims.exp.name())) {
            return null;
        }
        try {
            return claims.getLong(Claims.exp.name());
        } catch (IllegalArgumentException ex) {
            LOG.debugf("%s token expiry claim can not be converted to Long", tokenType);
            return null;
        }
    }
}
