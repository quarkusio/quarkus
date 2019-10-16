package io.quarkus.redis.it;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.lettuce.core.Value;

@Path("/quarkus-redis")
@ApplicationScoped
public class QuarkusRedisResource {
    @Inject
    SyncCommands syncCommands;

    @Inject
    AsyncCommands asyncCommands;

    @Inject
    ReactiveCommands reactiveCommands;

    // synchronous
    @GET
    @Path("/sync/{key}")
    public String getSync(@PathParam("key") String key) {
        return syncCommands.get(key);
    }

    @POST
    @Path("/sync/{key}")
    public void setSync(@PathParam("key") String key, String value) {
        this.syncCommands.set(key, value);
    }

    @POST
    @Path("/sync-bytes/{key}")
    public void setSyncBytes(@PathParam("key") String key, String value) {
        this.syncCommands.set(key, value.getBytes());
    }

    // hashes
    @POST
    @Path("/hashes/{key}")
    public void hset(@PathParam("key") String key, String value) {
        this.syncCommands.hset("hashes", key, value);
    }

    @GET
    @Path("/hashes/")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getHash() {
        return this.syncCommands.hgetall("hashes");
    }

    // list
    @POST
    @Path("/lists/")
    public void push(String value) {
        this.syncCommands.lpush("strings", value);
    }

    @GET
    @Path("/lists/")
    public String pop() {
        return this.syncCommands.lpop("strings");
    }

    // asynchronous
    @GET
    @Path("/async/{key}")
    public void getAsync(@PathParam("key") String key, @Suspended final AsyncResponse asyncResponse) {
        this.asyncCommands.get(key).thenAccept(value -> asyncResponse.resume(value));
    }

    @POST
    @Path("/async/{key}")
    public void setASync(@PathParam("key") String key, String value, @Suspended final AsyncResponse asyncResponse) {
        this.asyncCommands.set(key, value).thenAccept((x) -> asyncResponse.resume(Response.noContent().build()));
    }

    // reactive
    @GET
    @Path("/reactive/{key}")
    public void getReactive(@Suspended final AsyncResponse asyncResponse, @PathParam("key") String key) {
        this.reactiveCommands.get(key).single().subscribe((value) -> asyncResponse.resume(value));
    }

    @POST
    @Path("/reactive/{key}")
    public void setReactive(@PathParam("key") String key, String value, @Suspended final AsyncResponse asyncResponse) {
        this.reactiveCommands.set(key, value).single().subscribe((x) -> asyncResponse.resume(Response.noContent().build()));
    }

    // geos
    @GET
    @Path("/geos")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Value<String>> triggerGeoOperations() {
        syncCommands.geoadd("geos", 127.055249, 37.5075175, "a");
        syncCommands.geoadd("geos", 127.054251, 37.5053014, "b");
        syncCommands.geoadd("geos", 127.0643587, 37.5091197, "c");
        return syncCommands.geohash("geos");
    }

    // default methods
    @GET
    @Path("/default/{key}")
    public String getDefault(@PathParam("key") String key) {
        return syncCommands.defaultGet(key);
    }
}
