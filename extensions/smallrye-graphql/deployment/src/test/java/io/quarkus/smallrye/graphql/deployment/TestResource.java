package io.quarkus.smallrye.graphql.deployment;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLSchema;
import io.smallrye.graphql.api.Context;

/**
 * Just a test endpoint
 */
@GraphQLApi
public class TestResource {

    @Inject
    Context context;

    @Query
    public TestPojo ping() {
        return new TestPojo("pong");
    }

    @Query
    public TestPojo foo() {
        return new TestPojo("bar");
    }

    @Query
    public TestPojo[] foos() {
        return new TestPojo[] { foo() };
    }

    @Query("context")
    public String getPathFromContext() {
        return context.getPath();
    }

    @Query
    public TestGenericsPojo<String> generics() {
        return new TestGenericsPojo<>("I know it");
    }

    @Query
    public TestPojo businesserror() throws BusinessException {
        throw new BusinessException("Some invalid case");
    }

    @Query
    public TestPojo systemserror() {
        throw new RuntimeException("Some system problem");
    }

    @Mutation
    public TestPojo moo(String name) {
        return new TestPojo(name);
    }

    @Query
    public String testCharset(String characters) {
        return characters;
    }

    // <placeholder>

    public TestRandom getRandomNumber(@Source TestPojo testPojo) {
        return new TestRandom(123);
    }

    public GraphQLSchema.Builder addMyOwnEnum(@Observes GraphQLSchema.Builder builder) {

        GraphQLEnumType myOwnEnum = GraphQLEnumType.newEnum()
                .name("SomeEnum")
                .description("Adding some enum type")
                .value("value1")
                .value("value2").build();

        return builder.additionalType(myOwnEnum);
    }
}
