package io.quarkus.smallrye.graphql.deployment.federation.base;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class FooApi {
    @Query
    public Foo foo(int id) {
        var foo = new Foo();
        foo.id = id;
        foo.name = "Name of " + id;
        return foo;
    }
}
