package io.quarkus.it.smallrye.graphql;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

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
        return options;
    }

    @Name("options2")
    // make sure that returning Collection.class does not break native mode
    public Collection<Greeting> buildInOptionsCollection(@Source Greeting greeting) {
        List<Greeting> options = new ArrayList<>();
        options.add(new Morning());
        return options;
    }

    @Query
    public Farewell farewell() {
        return new Farewell();
    }

    @Inject
    FaultTolerantService service;

    @Query
    public String faultTolerance() {
        service.causeTimeout();
        return "PASSED";
    }
}
