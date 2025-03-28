package io.quarkus.it.compose.devservices.redis;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.redis.datasource.RedisDataSource;

@IfBuildProfile("redis")
@Path("/redis")
public class RedisEndpoint {

    @Inject
    RedisDataSource redisClient;

    @ConfigProperty(name = "quarkus.redis.hosts")
    String redisHosts;

    @ConfigProperty(name = "quarkus.redis.password")
    String redisPassword;

    @GET
    public List<String> keys() {
        return redisClient.key().keys("*");
    }

    @GET
    @Path("/{key}")
    public String get(String key) {
        return redisClient.value(String.class).get(key);
    }

    @PUT
    @Path("/{key}")
    public void set(String key, String value) {
        redisClient.value(String.class).set(key, value);
    }

    @GET
    @Path("/host")
    public String host() {
        return redisHosts;
    }

    @GET
    @Path("/password")
    public String password() {
        return redisPassword;
    }
}
