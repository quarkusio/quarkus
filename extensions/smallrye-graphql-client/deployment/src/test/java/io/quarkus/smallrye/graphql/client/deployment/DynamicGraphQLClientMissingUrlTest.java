package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public class DynamicGraphQLClientMissingUrlTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    @GraphQLClient("invalid")
    Instance<DynamicGraphQLClient> client;

    /**
     * Check that when URL is not defined for a dynamic client, the thrown error message suggests the
     * `quarkus.*` config property rather than the SmallRye property (`CLIENT_NAME/mp-graphql/url`). This
     * is achieved by using `io.quarkus.smallrye.graphql.client.runtime.QuarkifiedErrorMessageProvider`.
     */
    @Test
    public void checkErrorMessage() {
        try {
            client.get();
            fail("Injection of a dynamic client must fail because no URL is defined");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("quarkus.smallrye-graphql-client.invalid.url"));
        }
    }

}