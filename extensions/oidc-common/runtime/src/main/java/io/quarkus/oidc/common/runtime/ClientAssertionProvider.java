package io.quarkus.oidc.common.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public final class ClientAssertionProvider implements Closeable {

    private record ClientAssertion(String bearerToken, long expiresAt, long timerId) {
        private boolean isInvalid() {
            final long nowSecs = System.currentTimeMillis() / 1000;
            return nowSecs > expiresAt;
        }
    }

    private static final Logger LOG = Logger.getLogger(ClientAssertionProvider.class);
    private final Vertx vertx;
    private final Path bearerTokenPath;
    private volatile ClientAssertion clientAssertion;

    public ClientAssertionProvider(Vertx vertx, Path bearerTokenPath) {
        this.vertx = vertx;
        this.bearerTokenPath = bearerTokenPath;
        this.clientAssertion = loadFromFileSystem();
    }

    public String getClientAssertion() {
        ClientAssertion clientAssertion = this.clientAssertion;
        if (isInvalid(clientAssertion)) {
            clientAssertion = loadClientAssertion();
        }
        return clientAssertion == null ? null : clientAssertion.bearerToken;
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
                ClientAssertionProvider.this.clientAssertion = loadFromFileSystem();
            }
        });
    }

    private void cancelRefresh() {
        if (clientAssertion != null) {
            vertx.cancelTimer(clientAssertion.timerId);
        }
    }

    private ClientAssertion loadFromFileSystem() {
        if (Files.exists(bearerTokenPath)) {
            try {
                String bearerToken = Files.readString(bearerTokenPath).trim();
                if (bearerToken.isEmpty()) {
                    LOG.error(String.format("Bearer token file at path %s is empty or contains only whitespace",
                            bearerTokenPath));
                    return null;
                }
                Long expiresAt = getExpiresAtFromExpClaim(bearerToken);
                if (expiresAt != null) {
                    return new ClientAssertion(bearerToken, expiresAt, scheduleRefresh(expiresAt));
                } else {
                    LOG.error("Bearer token or its expiry claim is invalid");
                }
            } catch (IOException e) {
                LOG.error("Failed to read file with a bearer token at path: " + bearerTokenPath, e);
            }
        } else {
            LOG.warn("Cannot find a file with a bearer token at path: " + bearerTokenPath);
        }
        return null;
    }

    private static boolean isInvalid(ClientAssertion clientAssertion) {
        return clientAssertion == null || clientAssertion.isInvalid();
    }

    private static Long getExpiresAtFromExpClaim(String bearerToken) {
        JsonObject claims = OidcCommonUtils.decodeJwtContent(bearerToken);
        if (claims == null || !claims.containsKey(Claims.exp.name())) {
            return null;
        }
        try {
            return claims.getLong(Claims.exp.name());
        } catch (IllegalArgumentException ex) {
            LOG.debug("Bearer token expiry claim can not be converted to Long");
            return null;
        }
    }
}
