package io.quarkus.smallrye.graphql.deployment.federation.base.uni;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.smallrye.graphql.deployment.federation.base.Foo;
import io.smallrye.mutiny.Uni;

@GraphQLApi
public class FooApiUni {
    @Query
    public Uni<Foo> foo(int id) {
        var foo = new Foo();
        foo.id = id;
        foo.name = "Name of " + id;
        return Uni.createFrom().item(foo);
    }
}
