package io.quarkus.io.opentelemetry;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;

@Path("/redis")
public class RedisResource {

    private final ValueCommands<String, String> blocking;
    private final ReactiveValueCommands<String, String> reactive;
    private final RedisDataSource ds;
    private final ReactiveRedisDataSource reactiveDs;

    @Inject
    public RedisResource(RedisDataSource ds,
            ReactiveRedisDataSource reactiveDs) {
        this.blocking = ds.value(String.class);
        this.reactive = reactiveDs.value(String.class);
        this.ds = ds;
        this.reactiveDs = reactiveDs;
    }

    // synchronous
    @GET
    @Path("/sync/{key}")
    public String getSync(@PathParam("key") String key) {
        return blocking.get(key);
    }

    @POST
    @Path("/sync/{key}")
    public void setSync(@PathParam("key") String key, String value) {
        blocking.set(key, value);
    }

    @POST
    @Path("/sync/invalid-operation")
    public void getInvalidOperation() {
        ds.execute("bazinga");
    }

    // reactive
    @GET
    @Path("/reactive/{key}")
    public Uni<String> getReactive(@PathParam("key") String key) {
        return reactive.get(key);
    }

    @POST
    @Path("/reactive/{key}")
    public Uni<Void> setReactive(@PathParam("key") String key, String value) {
        return this.reactive.set(key, value);
    }

    @POST
    @Path("/reactive/invalid-operation")
    public Uni<Void> getReactiveInvalidOperation() {
        return reactiveDs.execute("bazinga")
                .replaceWithVoid();
    }

    // tainted
    @GET
    @Path("/tainted")
    public String getTainted() {
        ds.withConnection(conn -> {
            conn.select(7); // taints the connection
            conn.value(String.class).get("foobar");
        });
        return "OK";
    }
}
