package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.*;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.client.deployment.model.Person;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLApi;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLClientApi;
import io.quarkus.test.QuarkusUnitTest;

public class TypesafeGraphQLClientMissingUrlTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestingGraphQLApi.class, TestingGraphQLClientApi.class, Person.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    Instance<TestingGraphQLClientApi> client;

    /**
     * Check that when URL is not defined for a typesafe client, the thrown error message suggests the
     * `quarkus.*` config property rather than the SmallRye property (`CLIENT_NAME/mp-graphql/url`). This
     * is achieved by using `io.quarkus.smallrye.graphql.client.runtime.QuarkifiedErrorMessageProvider`.
     */
    @Test
    public void checkErrorMessage() {
        try {
            client.get();
            fail("Injection of a typesafe client must fail because no URL is defined");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("quarkus.smallrye-graphql-client.typesafeclient.url"));
        }
    }

}
