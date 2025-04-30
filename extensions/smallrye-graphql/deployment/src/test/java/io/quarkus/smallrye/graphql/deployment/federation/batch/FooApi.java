package io.quarkus.smallrye.graphql.deployment.federation.batch;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class FooApi {
    @Query
    public List<Foo> foos(List<Integer> id) {
        return id.stream().map(this::foo).collect(Collectors.toList());
    }

    private Foo foo(int id) {
        var foo = new Foo();
        foo.id = id;
        foo.name = "Name of " + id;
        return foo;
    }
}
