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

final class KubernetesServiceClientAssertionProvider implements ClientAssertionProvider, Closeable {

    record ClientAssertion(String bearerToken, long expiresAtMillis, long timerId) {
        private boolean isInvalid() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private static final Logger LOG = Logger.getLogger(KubernetesServiceClientAssertionProvider.class);
    private final Vertx vertx;
    private final Path bearerTokenPath;
    volatile ClientAssertion clientAssertion;

    KubernetesServiceClientAssertionProvider(Vertx vertx, Path bearerTokenPath) {
        this.vertx = vertx;
        this.bearerTokenPath = bearerTokenPath;
        this.clientAssertion = loadInitialClientAssertion();
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
    public void close() {
        cancelRefresh();
        clientAssertion = null;
    }

    private synchronized ClientAssertion loadClientAssertion() {
        if (isInvalid(clientAssertion)) {
            cancelRefresh();
            var newClientAssertion = loadFromFileSystem();
            rememberClientAssertion(newClientAssertion);
            return newClientAssertion;
        }
        return clientAssertion;
    }

    private long scheduleRefresh(long expiresInMillis) {
        // in K8 and OCP, tokens are proactively rotated at 80 % of their TTL
        long delay = (long) (expiresInMillis * 0.85);
        if (delay > 0) {
            return vertx.setTimer(delay, new Handler<Long>() {
                @Override
                public void handle(Long ignored) {
                    rememberClientAssertion(loadFromFileSystem());
                }
            });
        }
        return -1;
    }

    private void cancelRefresh() {
        if (clientAssertion != null) {
            cancelTimer(clientAssertion.timerId);
        }
    }

    private void cancelTimer(long timerId) {
        vertx.cancelTimer(timerId);
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
                Long expiresAtMillis = getExpiresAtFromExpClaim(bearerToken);
                if (expiresAtMillis != null) {
                    long expiresInMillis = expiresAtMillis - System.currentTimeMillis();
                    if (expiresInMillis > 0) {
                        return new ClientAssertion(bearerToken, expiresAtMillis, scheduleRefresh(expiresInMillis));
                    } else {
                        LOG.error("Bearer token loaded from filesystem has already expired");
                    }
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
        if (claims != null) {
            try {
                Long expAtSeconds = claims.getLong(Claims.exp.name());
                if (expAtSeconds != null) {
                    return expAtSeconds * 1000L;
                }
            } catch (IllegalArgumentException ex) {
                LOG.debug("Bearer token expiry claim can not be converted to Long");
            }
        }
        return null;
    }

    private synchronized void rememberClientAssertion(ClientAssertion newClientAssertion) {
        if (shouldReuseClientAssertion(newClientAssertion)) {
            var oldClientAssertion = this.clientAssertion;
            if (oldClientAssertion == null) {
                this.clientAssertion = newClientAssertion;
            } else if (newClientAssertion.expiresAtMillis >= oldClientAssertion.expiresAtMillis) {
                cancelTimer(oldClientAssertion.timerId);
                this.clientAssertion = newClientAssertion;
            } else {
                cancelTimer(newClientAssertion.timerId);
            }
        }
    }

    private static boolean shouldReuseClientAssertion(ClientAssertion clientAssertion) {
        return clientAssertion != null && clientAssertion.timerId >= 0;
    }

    private ClientAssertion loadInitialClientAssertion() {
        var newClientAssertion = loadFromFileSystem();
        return shouldReuseClientAssertion(newClientAssertion) ? newClientAssertion : null;
    }
}
