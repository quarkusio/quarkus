package io.quarkus.it.legacy.redirect;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

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

    @Name("options")
    public List<Greeting> buildInOptions(@Source Greeting greeting) {
        List<Greeting> options = new ArrayList<>();
        options.add(new Hello());
        options.add(new Morning());
        return options;
    }

    @Query
    public Farewell farewell() {
        return new Farewell();
    }
}
