package io.quarkus.redis.client.deployment.devmode;

import java.util.Arrays;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

@Path("/inc")
public class IncrementResource {

    public static final int INCREMENT = 1;
    @Inject
    RedisClient redisClient;

    @GET
    public int increment() {
        Response response = redisClient.get("key");
        Integer resp = 0;
        if (response != null) {
            resp = response.toInteger();
        }
        resp = resp + INCREMENT;
        redisClient.set(Arrays.asList("key", Integer.toString(resp)));
        return resp;
    }

}
