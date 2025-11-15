package io.quarkus.oidc.db.token.state.manager.runtime;

import static io.quarkus.oidc.runtime.CodeAuthenticationMechanism.SESSION_MAX_AGE_PARAM;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class OidcDbTokenStateManager implements TokenStateManager {

    private static final Logger LOG = Logger.getLogger(OidcDbTokenStateManager.class);
    private static final String TOKEN_STATE_INSERT_FAILED = "Failed to insert token state into database";
    private static final String FAILED_TO_ACQUIRE_TOKEN = "Failed to acquire authorization code tokens";

    private static final String ID_TOKEN_COLUMN = "id_token";
    private static final String ACCESS_TOKEN_COLUMN = "access_token";
    private static final String ACCESS_TOKEN_EXPIRES_IN_COLUMN = "access_token_expires_in";
    private static final String ACCESS_TOKEN_SCOPE_COLUMN = "access_token_scope";
    private static final String REFRESH_TOKEN_COLUMN = "refresh_token";

    private final String insertStatement;
    private final String deleteStatement;
    private final String getQuery;
    private Pool pool;

    OidcDbTokenStateManager(String insertStatement, String deleteStatement, String getQuery) {
        this.insertStatement = insertStatement;
        this.deleteStatement = deleteStatement;
        this.getQuery = getQuery;
    }

    void setSqlClientPool(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Uni<String> createTokenState(RoutingContext event, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {
        final String id = now() + UUID.randomUUID().toString();
        return Uni
                .createFrom()
                .completionStage(
                        pool
                                .withTransaction(client -> client
                                        .preparedQuery(insertStatement)
                                        .execute(
                                                Tuple.of(tokens.getIdToken(), tokens.getAccessToken(),
                                                        tokens.getRefreshToken(), tokens.getAccessTokenExpiresIn(),
                                                        tokens.getAccessTokenScope(),
                                                        expiresIn(event), id)))
                                .toCompletionStage())
                .onFailure().transform(new Function<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable throwable) {
                        return new AuthenticationFailedException(TOKEN_STATE_INSERT_FAILED, throwable);
                    }
                })
                .flatMap(new Function<RowSet<Row>, Uni<? extends String>>() {
                    @Override
                    public Uni<? extends String> apply(RowSet<Row> rows) {
                        if (rows != null) {
                            return Uni.createFrom().item(id);
                        }
                        return Uni.createFrom().failure(new AuthenticationFailedException(TOKEN_STATE_INSERT_FAILED));
                    }
                })
                .memoize().indefinitely();
    }

    @Override
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        return Uni
                .createFrom()
                .completionStage(
                        pool
                                .preparedQuery(getQuery)
                                .execute(Tuple.of(tokenState))
                                .toCompletionStage())
                .onFailure().transform(new Function<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable throwable) {
                        return new AuthenticationCompletionException(FAILED_TO_ACQUIRE_TOKEN, throwable);
                    }
                })
                .flatMap(new Function<RowSet<Row>, Uni<? extends AuthorizationCodeTokens>>() {
                    @Override
                    public Uni<? extends AuthorizationCodeTokens> apply(RowSet<Row> rows) {
                        if (rows != null) {
                            final RowIterator<Row> iterator = rows.iterator();
                            if (iterator.hasNext()) {
                                final Row firstRow = iterator.next();
                                return Uni
                                        .createFrom()
                                        .item(new AuthorizationCodeTokens(
                                                firstRow.getString(ID_TOKEN_COLUMN),
                                                firstRow.getString(ACCESS_TOKEN_COLUMN),
                                                firstRow.getString(REFRESH_TOKEN_COLUMN),
                                                firstRow.getLong(ACCESS_TOKEN_EXPIRES_IN_COLUMN),
                                                firstRow.getString(ACCESS_TOKEN_SCOPE_COLUMN)));
                            }
                        }
                        return Uni.createFrom().failure(new AuthenticationCompletionException(FAILED_TO_ACQUIRE_TOKEN));
                    }
                })
                .memoize().indefinitely();
    }

    @Override
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        return Uni
                .createFrom()
                .completionStage(pool
                        .preparedQuery(deleteStatement)
                        .execute(Tuple.of(tokenState))
                        .toCompletionStage())
                .replaceWithVoid()
                .onFailure()
                .recoverWithItem(new Function<Throwable, Void>() {
                    @Override
                    public Void apply(Throwable throwable) {
                        LOG.debugf("Failed to delete tokens: %s", throwable.getMessage());
                        return null;
                    }
                });
    }

    static long now() {
        return Instant.now().getEpochSecond();
    }

    private static long expiresIn(RoutingContext event) {
        return now() + event.<Long> get(SESSION_MAX_AGE_PARAM);
    }
}
