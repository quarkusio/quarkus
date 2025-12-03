package io.quarkus.oidc.db.token.state.manager.runtime;

import static io.quarkus.oidc.db.token.state.manager.runtime.OidcDbTokenStateManager.now;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

@Singleton
public final class OidcDbTokenStateManagerInitializer {

    private static final Logger LOG = Logger.getLogger(OidcDbTokenStateManagerInitializer.class);
    private static final String FAILED_TO_CREATE_DB_TABLE = "unknown reason, please report the issue and create table manually";
    /**
     * Extra 30 seconds before we delete expired tokens.
     */
    private static final long EXPIRED_EXTRA_GRACE = 30;
    private volatile Long timerId = null;
    private volatile SupportedReactiveSqlClient supportedReactiveSqlClient = null;

    void setSupportedReactiveSqlClient(SupportedReactiveSqlClient supportedReactiveSqlClient) {
        this.supportedReactiveSqlClient = supportedReactiveSqlClient;
    }

    void initialize(@Observes StartupEvent event, OidcDbTokenStateManagerRunTimeConfig config, Vertx vertx, Pool pool) {
        Objects.requireNonNull(supportedReactiveSqlClient);
        if (config.createDatabaseTableIfNotExists()) {
            createDatabaseTable(pool, supportedReactiveSqlClient.getCreateTableDdl(config),
                    supportedReactiveSqlClient.supportsIfTableNotExists);
        }
        periodicallyDeleteExpiredTokens(vertx, pool, config.deleteExpiredDelay().toMillis());
    }

    void shutdown(@Observes ShutdownEvent event, Vertx vertx) {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
    }

    private void periodicallyDeleteExpiredTokens(Vertx vertx, Pool pool, long delayBetweenChecks) {
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
                                LOG.debug("Failed to create database table", throwable);
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

    public enum SupportedReactiveSqlClient {
        POSTGRESQL(true,
                (int idToken, int accessToken, int refreshToken) -> "CREATE TABLE IF NOT EXISTS oidc_db_token_state_manager ("
                        + "id VARCHAR(100) PRIMARY KEY, "
                        + "id_token VARCHAR(" + idToken + "), "
                        + "access_token VARCHAR(" + accessToken + "), "
                        + "refresh_token VARCHAR(" + refreshToken + "), "
                        + "access_token_expires_in BIGINT, "
                        + "access_token_scope VARCHAR, "
                        + "expires_in BIGINT NOT NULL)"),
        DB2(false, (int idToken, int accessToken, int refreshToken) -> "CREATE TABLE oidc_db_token_state_manager ("
                + "id VARCHAR(100) NOT NULL PRIMARY KEY, "
                + "id_token VARCHAR(" + idToken + "), "
                + "access_token VARCHAR(" + accessToken + "), "
                + "refresh_token VARCHAR(" + refreshToken + "), "
                + "access_token_expires_in BIGINT, "
                + "access_token_scope VARCHAR(100), "
                + "expires_in BIGINT NOT NULL)"),
        ORACLE(true,
                (int idToken, int accessToken, int refreshToken) -> "CREATE TABLE IF NOT EXISTS oidc_db_token_state_manager ("
                        + "id VARCHAR2(100), "
                        + "id_token VARCHAR2(" + idToken + "), "
                        + "access_token VARCHAR2(" + accessToken + "), "
                        + "refresh_token VARCHAR2(" + refreshToken + "), "
                        + "access_token_expires_in NUMBER, "
                        + "access_token_scope VARCHAR2(100), "
                        + "expires_in NUMBER NOT NULL, "
                        + "PRIMARY KEY (id))"),
        MYSQL(true,
                (int idToken, int accessToken, int refreshToken) -> "CREATE TABLE IF NOT EXISTS oidc_db_token_state_manager ("
                        + "id VARCHAR(100), "
                        + "id_token VARCHAR(" + idToken + ") NULL, "
                        + "access_token VARCHAR(" + accessToken + ") NULL, "
                        + "refresh_token VARCHAR(" + refreshToken + ") NULL, "
                        + "access_token_expires_in BIGINT NULL, "
                        + "access_token_scope VARCHAR(100) NULL, "
                        + "expires_in BIGINT NOT NULL, "
                        + "PRIMARY KEY (id))"),
        MSSQL(false, new CreateTableDdlProvider() {

            @Override
            public String getCreateTableDdl(int idToken, int accessToken, int refreshToken) {
                return "CREATE TABLE oidc_db_token_state_manager ("
                        + "id NVARCHAR(100) PRIMARY KEY, "
                        + "id_token NVARCHAR(" + getColumnSize(idToken) + "), "
                        + "access_token NVARCHAR(" + getColumnSize(accessToken) + "), "
                        + "refresh_token NVARCHAR(" + getColumnSize(refreshToken) + "), "
                        + "access_token_expires_in BIGINT, "
                        + "access_token_scope NVARCHAR(100), "
                        + "expires_in BIGINT NOT NULL)";
            }

            private static String getColumnSize(int columnSize) {
                // from SQL Server docs:
                // - the value of n defines the string size in byte-pairs, and can be from 1 through 4000
                // - 'max' indicates that the maximum storage size is 2^31-1 characters (2 GB)
                // therefore if someone tries to set more than maximum 4000 byte-pairs, we simply use 'max'
                if (columnSize > 4000) {
                    return "max";
                }
                return Integer.toString(columnSize);
            }
        });

        private final boolean supportsIfTableNotExists;
        private final CreateTableDdlProvider createTableDdlProvider;

        SupportedReactiveSqlClient(boolean supportsIfTableNotExists, CreateTableDdlProvider createTableDdlProvider) {
            this.supportsIfTableNotExists = supportsIfTableNotExists;
            this.createTableDdlProvider = createTableDdlProvider;
        }

        private String getCreateTableDdl(OidcDbTokenStateManagerRunTimeConfig config) {
            return createTableDdlProvider.getCreateTableDdl(config.idTokenColumnSize(), config.accessTokenColumnSize(),
                    config.refreshTokenColumnSize());
        }

        private interface CreateTableDdlProvider {
            String getCreateTableDdl(int idToken, int accessToken, int refreshToken);
        }
    }
}
