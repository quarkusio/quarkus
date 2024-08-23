package io.quarkus.redis.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;

@Path("/quarkus-redis/custom-codec")
@ApplicationScoped
public class CustomCodecResource {

    private final ValueCommands<String, Person> values;

    public CustomCodecResource(RedisDataSource ds) {
        values = ds.value(Person.class);
    }

    // synchronous
    @GET
    @Path("/{key}")
    public Person getSync(@PathParam("key") String key) {
        return values.get(key);
    }

    @POST
    @Path("/{key}")
    public void setSync(@PathParam("key") String key, Person value) {
        values.set(key, value);
    }

}
