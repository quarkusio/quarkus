package io.quarkus.oidc.db.token.state.manager.runtime;

import static io.quarkus.oidc.db.token.state.manager.runtime.OidcDbTokenStateManager.now;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class OidcDbTokenStateManagerInitializer {

    private static final Logger LOG = Logger.getLogger(OidcDbTokenStateManagerInitializer.class);
    private static final String FAILED_TO_CREATE_DB_TABLE = "unknown reason, please report the issue and create table manually";
    /**
     * Extra 30 seconds before we delete expired tokens.
     */
    private static final long EXPIRED_EXTRA_GRACE = 30;
    private static volatile Long timerId = null;

    void initialize(@Observes StartupEvent event, OidcDbTokenStateManagerRunTimeConfig config, Vertx vertx, Pool pool,
            OidcDbTokenStateManagerInitializerProperties initializerProps) {
        if (config.createDatabaseTableIfNotExists()) {
            createDatabaseTable(pool, initializerProps.createTableDdl, initializerProps.supportsIfTableNotExists);
        }
        periodicallyDeleteExpiredTokens(vertx, pool, config.deleteExpiredDelay().toMillis());
    }

    void shutdown(@Observes ShutdownEvent event, Vertx vertx) {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
    }

    private static void periodicallyDeleteExpiredTokens(Vertx vertx, Pool pool, long delayBetweenChecks) {
        timerId = vertx
                .setPeriodic(5000, delayBetweenChecks, new Handler<Long>() {

                    private final AtomicBoolean deleteInProgress = new AtomicBoolean(false);

                    @Override
                    public void handle(Long aLong) {
                        if (deleteInProgress.compareAndSet(false, true)) {

                            final long deleteExpiresIn = now() - EXPIRED_EXTRA_GRACE;
                            Uni.createFrom().completionStage(
                                    pool
                                            .query("DELETE FROM oidc_db_token_state_manager WHERE expires_in < "
                                                    + deleteExpiresIn)
                                            .execute()
                                            .toCompletionStage())
                                    .subscribe()
                                    .with(
                                            new Consumer<RowSet<Row>>() {
                                                @Override
                                                public void accept(RowSet<Row> ignored) {
                                                    // success
                                                    deleteInProgress.set(false);
                                                }
                                            },
                                            new Consumer<Throwable>() {
                                                @Override
                                                public void accept(Throwable t) {
                                                    LOG.errorf("Failed to expired OIDC token states from database: %s",
                                                            t.getMessage());
                                                    deleteInProgress.set(false);
                                                }
                                            });
                        }
                    }
                });
    }

    private static void createDatabaseTable(Pool pool, String createTableDdl, boolean supportsIfTableNotExists) {
        LOG.debugf("Creating database table with query: %s", createTableDdl);
        String errMsg = Uni
                .createFrom()
                .completionStage(
                        pool
                                .query(createTableDdl)
                                .execute()
                                .toCompletionStage())
                .onItemOrFailure()
                .transformToUni(new BiFunction<RowSet<Row>, Throwable, Uni<? extends String>>() {
                    @Override
                    public Uni<String> apply(RowSet<Row> rows, Throwable throwable) {
                        if (throwable != null) {
                            if (supportsIfTableNotExists) {
                                return Uni.createFrom().item(throwable.getMessage());
                            } else {
                                // most likely we tried to create table even though it already exists
                                return Uni.createFrom().nullItem();
                            }
                        }
                        // assert table exists
                        return Uni
                                .createFrom()
                                .completionStage(pool
                                        // use MAX in order to limit response size
                                        // and LIMIT clause is not supported by all the databases
                                        .query("SELECT MAX(id) FROM oidc_db_token_state_manager")
                                        .execute()
                                        .toCompletionStage())
                                .map(new Function<RowSet<Row>, String>() {
                                    @Override
                                    public String apply(RowSet<Row> rows) {
                                        if (rows != null && rows.columnsNames().size() == 1) {
                                            // table exists
                                            return null;
                                        }
                                        // table does not exist
                                        return FAILED_TO_CREATE_DB_TABLE;
                                    }
                                })
                                .onFailure().recoverWithItem(new Function<Throwable, String>() {
                                    @Override
                                    public String apply(Throwable throwable) {
                                        LOG.error("Create database query failed with: ", throwable);
                                        return FAILED_TO_CREATE_DB_TABLE;
                                    }
                                });
                    }
                })
                .await()
                .indefinitely();
        if (errMsg != null) {
            throw new RuntimeException("OIDC Token State Manager failed to create database table: " + errMsg);
        }
    }

    public static final class OidcDbTokenStateManagerInitializerProperties {

        private final String createTableDdl;
        private final boolean supportsIfTableNotExists;

        OidcDbTokenStateManagerInitializerProperties(String createTableDdl, boolean supportsIfTableNotExists) {
            this.createTableDdl = createTableDdl;
            this.supportsIfTableNotExists = supportsIfTableNotExists;
        }
    }
}
