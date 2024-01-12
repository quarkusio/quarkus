package io.quarkus.virtual.redis;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/")
@RunOnVirtualThread
public class RedisResource {

    private final HashCommands<String, String, String> hash;

    public RedisResource(RedisDataSource ds) {
        hash = ds.hash(String.class);
    }

    @GET
    public String testRedis() {
        VirtualThreadsAssertions.assertEverything();
        String value = UUID.randomUUID().toString();
        hash.hset("test", "test", value);

        var retrieved = hash.hget("test", "test");
        if (!retrieved.equals(value)) {
            throw new IllegalStateException("Something wrong happened: " + retrieved + " != " + value);
        }
        return "OK";
    }

    @GET
    @CacheResult(cacheName = "my-cache")
    @Path("/cached")
    public String testCache() {
        return UUID.randomUUID().toString();
    }

}
