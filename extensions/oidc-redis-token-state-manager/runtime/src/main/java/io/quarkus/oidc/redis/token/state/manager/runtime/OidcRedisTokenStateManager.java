package io.quarkus.oidc.redis.token.state.manager.runtime;

import static io.quarkus.oidc.runtime.CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class OidcRedisTokenStateManager implements TokenStateManager {

    private static final String REDIS_KEY_PREFIX = "oidc:token:";
    private final ReactiveRedisDataSource dataSource;

    OidcRedisTokenStateManager(ReactiveRedisDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Uni<String> createTokenState(RoutingContext event, OidcTenantConfig oidcConfig, AuthorizationCodeTokens tokens,
            OidcRequestContext<String> requestContext) {
        return createTokenState(AuthorizationCodeTokensRecord.of(tokens), 0, newSetArgs(event))
                .onFailure().transform(AuthenticationFailedException::new);
    }

    @Override
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        return dataSource.value(AuthorizationCodeTokensRecord.class).get(toTokenKey(tokenState))
                .onItem().ifNotNull().transform(AuthorizationCodeTokensRecord::toTokens)
                .onFailure().transform(AuthenticationCompletionException::new);
    }

    @Override
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        return dataSource.key(String.class).del(toTokenKey(tokenState)).onFailure().recoverWithNull().replaceWithVoid();
    }

    private Uni<String> createTokenState(AuthorizationCodeTokensRecord tokens, int attemptCount, SetArgs setArgs) {
        if (attemptCount >= 3) {
            return Uni.createFrom().failure(
                    new RuntimeException("Failed to store OIDC token state in Redis as generated key already existed"));
        }
        final String tokenState = UUID.randomUUID().toString();
        return dataSource
                .value(AuthorizationCodeTokensRecord.class)
                .setGet(toTokenKey(tokenState), tokens, setArgs)
                .flatMap(new Function<AuthorizationCodeTokensRecord, Uni<? extends String>>() {
                    @Override
                    public Uni<? extends String> apply(AuthorizationCodeTokensRecord previousValue) {
                        if (previousValue == null) {
                            // value stored
                            return Uni.createFrom().item(tokenState);
                        }
                        // record key already existed, let's try again
                        return createTokenState(tokens, attemptCount + 1, setArgs);
                    }
                });
    }

    private static String toTokenKey(String tokenState) {
        return REDIS_KEY_PREFIX + tokenState;
    }

    private static SetArgs newSetArgs(RoutingContext event) {
        return new SetArgs().nx().exAt(expiresAt(event));
    }

    private static Instant expiresAt(RoutingContext event) {
        return Instant.now().plusSeconds(event.<Long> get(SESSION_MAX_AGE_PARAM));
    }

    record AuthorizationCodeTokensRecord(String idToken, String accessToken, String refreshToken, Long accessTokenExpiresIn,
            String accessTokenScope) {

        private static AuthorizationCodeTokensRecord of(AuthorizationCodeTokens tokens) {
            return new AuthorizationCodeTokensRecord(tokens.getIdToken(), tokens.getAccessToken(), tokens.getRefreshToken(),
                    tokens.getAccessTokenExpiresIn(), tokens.getAccessTokenScope());
        }

        private AuthorizationCodeTokens toTokens() {
            return new AuthorizationCodeTokens(idToken, accessToken, refreshToken, accessTokenExpiresIn, accessTokenScope);
        }
    }
}
