package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/optional-query/greetings")
public class OptionalQueryParamResource {

    public static final String HELLO = "hello ";
    public static final String NOBODY = "nobody";
    public static final String AND = " and ";

    @Path("/one")
    @GET
    public String sayHelloToValue(@QueryParam("name") final Optional<String> name) {
        return HELLO + name.orElse(NOBODY);
    }

    @Path("/list")
    @GET
    public String sayHelloToList(@QueryParam("name") final Optional<List<String>> names) {
        return doSayHelloToCollection(names);
    }

    @Path("/set")
    @GET
    public String sayHelloToSet(@QueryParam("name") final Optional<Set<String>> names) {
        return doSayHelloToCollection(names);
    }

    @Path("/sortedset")
    @GET
    public String sayHelloToSortedSet(@QueryParam("name") final Optional<SortedSet<String>> names) {
        return doSayHelloToCollection(names);
    }

    private String doSayHelloToCollection(final Optional<? extends Collection<String>> names) {
        return HELLO + names.map(l -> l.stream().collect(Collectors.joining(AND)))
                .orElse(NOBODY);
    }
}
