package io.quarkus.vertx.http.security.token;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.setRoutingContextAttribute;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * In-memory identity provider for the {@link OneTimeTokenAuthenticationRequest} request.
 * The one-time authentication tokens are hashed and stored in memory.
 */
final class OneTimeTokenInMemoryAuthenticator implements OneTimeTokenAuthenticator {

    private final Map<String, AuthTokenInfo> tokenStorage;
    private final IdentityProviderManager identityProviderManager;
    private final long periodicCleanupTaskId;

    OneTimeTokenInMemoryAuthenticator(IdentityProviderManager identityProviderManager, Vertx vertx) {
        this.identityProviderManager = identityProviderManager;
        this.tokenStorage = new ConcurrentHashMap<>();
        // clean-up is done every 20 seconds
        periodicCleanupTaskId = vertx.setPeriodic(20000, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                if (tokenStorage.isEmpty()) {
                    return;
                }
                Instant now = Instant.now();
                Set<String> keys = new HashSet<>();
                for (Map.Entry<String, AuthTokenInfo> e : tokenStorage.entrySet()) {
                    Instant expiresAt = e.getValue().expiresAt();
                    if (expiresAt.isAfter(now)) {
                        keys.add(e.getKey());
                    }
                }
                if (!keys.isEmpty()) {
                    for (String key : keys) {
                        tokenStorage.remove(key);
                    }
                }
            }
        });
    }

    @Override
    public Class<OneTimeTokenAuthenticationRequest> getRequestType() {
        return OneTimeTokenAuthenticationRequest.class;
    }

    @Override
    public Uni<Void> store(SecurityIdentity securityIdentity, PasswordCredential oneTimeTokenCredential,
            RequestInfo requestInfo) {
        store(securityIdentity.getPrincipal().getName(), oneTimeTokenCredential.getPassword(), requestInfo);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<SecurityIdentity> authenticate(OneTimeTokenAuthenticationRequest request,
            AuthenticationRequestContext authenticationRequestContext) {
        String oneTimeAuthTokenHash = hash(request.getCredential().getPassword());
        AuthTokenInfo authTokenInfo = tokenStorage.remove(oneTimeAuthTokenHash);
        if (authTokenInfo == null) {
            return Uni.createFrom().failure(new AuthenticationFailedException("Unknown one-time authentication token"));
        }
        if (Instant.now().isAfter(authTokenInfo.expiresAt)) {
            return Uni.createFrom().failure(new AuthenticationFailedException("One-time authentication token has expired"));
        }
        if (authTokenInfo.redirectLocation != null) {
            RoutingContext routingContext = getRoutingContextAttribute(request);
            routingContext.put(REDIRECT_LOCATION_KEY, authTokenInfo.redirectLocation);
        }
        AuthenticationRequest trustedAuthRequest = setRoutingContextAttribute(new TrustedAuthenticationRequest(
                authTokenInfo.userPrincipalName), getRoutingContextAttribute(request));
        return identityProviderManager.authenticate(trustedAuthRequest);
    }

    @Override
    public int priority() {
        // needs lower priority than any other provider for this authentication request
        return IdentityProvider.SYSTEM_LAST + 30;
    }

    void shutdown(@Observes ShutdownEvent event, Vertx vertx) {
        vertx.cancelTimer(periodicCleanupTaskId);
    }

    private synchronized void store(String userPrincipalName, String oneTimeTokenHash, RequestInfo requestInfo) {
        // even though the token starts with user principal hash, and we allow one token per user
        // we need to check for hash collisions
        if (tokenStorage.containsKey(oneTimeTokenHash)) {
            throw new IllegalArgumentException("Generated one-time authentication token already exists");
        }

        // one one-time token is allowed per user
        String previousOneTimeTokenHash = null;
        for (Map.Entry<String, AuthTokenInfo> e : tokenStorage.entrySet()) {
            if (userPrincipalName.equals(e.getValue().userPrincipalName)) {
                previousOneTimeTokenHash = e.getKey();
                break;
            }
        }
        if (previousOneTimeTokenHash != null) {
            tokenStorage.remove(previousOneTimeTokenHash);
        }

        Instant expiresAt = Instant.now().plus(requestInfo.expiresIn());
        var userPrincipalAndExpiration = new AuthTokenInfo(userPrincipalName, expiresAt, requestInfo.redirectLocation());
        tokenStorage.put(oneTimeTokenHash, userPrincipalAndExpiration);
    }

    private void store(String userPrincipalName, char[] oneTimeToken, RequestInfo requestInfo) {
        store(userPrincipalName, hash(oneTimeToken), requestInfo);
    }

    private static String hash(char[] oneTimeToken) {
        return HashUtil.sha512(toBytes(oneTimeToken));
    }

    private static byte[] toBytes(char[] chars) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 1);
        return bytes;
    }

    private record AuthTokenInfo(String userPrincipalName, Instant expiresAt, String redirectLocation) {
    }
}
