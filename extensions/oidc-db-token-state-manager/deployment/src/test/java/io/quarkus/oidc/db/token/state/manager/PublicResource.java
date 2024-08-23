package io.quarkus.oidc.db.token.state.manager;

import java.util.function.Function;

import javax.annotation.security.PermitAll;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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

}
