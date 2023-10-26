package io.quarkus.smallrye.graphql.deployment.federation.batch.uni;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.smallrye.graphql.deployment.federation.batch.Foo;
import io.smallrye.mutiny.Uni;

@GraphQLApi
public class FooApiUni {

    @Query
    public Uni<List<Foo>> foos(List<Integer> id) {
        return Uni.join().all(id.stream().map(this::foo).collect(Collectors.toList())).andFailFast();
    }

    private Uni<Foo> foo(int id) {
        var foo = new Foo();
        foo.id = id;
        foo.name = "Name of " + id;
        return Uni.createFrom().item(foo);
    }
}
