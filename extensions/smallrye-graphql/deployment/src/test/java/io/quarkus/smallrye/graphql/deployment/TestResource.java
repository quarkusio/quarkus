package io.quarkus.smallrye.graphql.deployment;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLSchema;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.graphql.api.Context;
import io.smallrye.mutiny.Uni;

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
    @NonBlocking
    public Uni<String> failureUniNonBlocking() {
        return Uni.createFrom().failure(new BusinessException("boom"));
    }

    @Query
    @Blocking
    public Uni<String> failureUniBlocking() {
        return Uni.createFrom().failure(new BusinessException("boom"));
    }

    @Query
    @NonBlocking
    public String failureSyncNonBlocking() throws BusinessException {
        throw new BusinessException("boom");
    }

    @Query
    @Blocking
    public String failureSyncBlocking() throws BusinessException {
        throw new BusinessException("boom");
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

    @Query
    public TestUnion testUnion() {
        return new TestUnionMember("what is my name");
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
