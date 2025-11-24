package io.quarkus.oidc.db.token.state.manager;

import java.util.function.Function;

import javax.annotation.security.PermitAll;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

@Path("/public")
@PermitAll
public class PublicResource {

    @Inject
    Pool pool;

    @Path("/db-state-manager-table-content")
    @GET
    public Uni<Long> getDbStateManagerRowsCount() {
        return Uni.createFrom().completionStage(pool
                .query("SELECT COUNT(*) FROM oidc_db_token_state_manager")
                .execute()
                .map(new Function<RowSet<Row>, Long>() {
                    @Override
                    public Long apply(RowSet<Row> rows) {
                        if (rows != null) {
                            var iterator = rows.iterator();
                            if (iterator.hasNext()) {
                                return iterator.next().getLong(0);
                            }
                        }
                        return 0L;
                    }
                })
                .toCompletionStage());
    }

    @Path("/db-state-manager-tokens")
    @GET
    public Uni<String> getDbStateManagerTokens() {
        return Uni.createFrom().completionStage(pool
                .query("SELECT id_token, access_token, refresh_token FROM oidc_db_token_state_manager")
                .execute()
                .map(new Function<RowSet<Row>, String>() {
                    @Override
                    public String apply(RowSet<Row> rows) {
                        if (rows != null) {
                            var iterator = rows.iterator();
                            if (iterator.hasNext()) {
                                Row row = iterator.next();
                                return """
                                        id token encrypted: %b, access token encrypted: %b, refresh token encrypted: %b
                                        """.formatted(OidcUtils.isEncryptedToken(row.getString(0)),
                                        OidcUtils.isEncryptedToken(row.getString(1)),
                                        OidcUtils.isEncryptedToken(row.getString(2))).trim();
                            }
                        }
                        return "";
                    }
                })
                .toCompletionStage());
    }

}
