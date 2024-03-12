package io.quarkus.smallrye.graphql.deployment.federation.complex;

import io.smallrye.graphql.api.federation.Extends;
import io.smallrye.graphql.api.federation.External;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Key;

@Key(fields = @FieldSet("id"))
@Extends
public class Foo {

    @External
    public int id;

    public String name;

}
