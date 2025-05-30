package io.quarkus.test.devservices.redis;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.redis.datasource.RedisDataSource;

@Path("/")
public class TestResource {

    @Inject
    RedisDataSource redis;

    @GET
    @Path("/ping")
    public String ping() {
        return redis.execute("ping").toString();
    }

    @GET
    @Path("/set/{key}/{value}")
    public String set(@PathParam("key") String key, @PathParam("value") String value) {
        redis.value(String.class).set(key, value);
        return "OK";
    }

    @GET
    @Path("/get/{key}")
    public String get(@PathParam("key") String key) {
        return redis.value(String.class).get(key);
    }
}
