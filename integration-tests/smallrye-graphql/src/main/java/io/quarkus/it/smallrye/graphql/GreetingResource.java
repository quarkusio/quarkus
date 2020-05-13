package io.quarkus.it.smallrye.graphql;

import java.time.LocalTime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class GreetingResource {

    @ConfigProperty(name = "message")
    String message;

    @Query
    public String message() {
        return message;
    }

    @Query
    public Greeting hello() {
        return new Greeting("hello", LocalTime.of(11, 34));
    }

    @Mutation
    public Greetings load(Greetings greetings) {
        return greetings;
    }
}
