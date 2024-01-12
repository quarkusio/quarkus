package io.quarkus.smallrye.graphql.deployment.federation.complex;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.mutiny.Uni;

@GraphQLApi
public class FooApi {
    @Query
    public Foo foo(int id) {
        var foo = new Foo();
        foo.id = id;
        foo.name = "Name of " + id;
        return foo;
    }

    @Query
    public Uni<List<Bar>> bars(List<Integer> id, List<String> otherId) {
        return Uni.join().all(
                IntStream.range(0, id.size()).boxed().map(i -> bar(id.get(i), otherId.get(i))).collect(Collectors.toList()))
                .andFailFast();
    }

    private Uni<Bar> bar(int id, String otherId) {
        var bar = new Bar();
        bar.id = id;
        bar.otherId = otherId;
        bar.name = id + otherId;
        return Uni.createFrom().item(bar);
    }
}
