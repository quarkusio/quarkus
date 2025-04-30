package io.quarkus.oidc.redis.token.state.manager.deployment;

import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.smallrye.mutiny.Uni;

@Path("/public")
public class PublicResource {

    private final ReactiveRedisDataSource dataSource;

    PublicResource(@ConfigProperty(name = "test.redis-client-name") Optional<String> clientName) {
        if (clientName.isEmpty()) {
            dataSource = Arc.container().select(ReactiveRedisDataSource.class).get();
        } else {
            dataSource = Arc.container()
                    .select(ReactiveRedisDataSource.class, RedisClientName.Literal.of(clientName.get())).get();
        }
    }

    @Path("oidc-token-states-count")
    @GET
    public Uni<Long> countOidcTokenStates() {
        return dataSource.key().keys("oidc:token:*").map(l -> (long) l.size());
    }

}
