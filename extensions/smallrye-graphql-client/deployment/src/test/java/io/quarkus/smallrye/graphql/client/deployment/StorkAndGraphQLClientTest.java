package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.client.deployment.model.Person;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLApi;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLClientApi;
import io.quarkus.test.QuarkusUnitTest;

public class StorkAndGraphQLClientTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestingGraphQLApi.class, TestingGraphQLClientApi.class, Person.class)
                    .addAsResource(new StringAsset(
                            "quarkus.smallrye-graphql-client.typesafeclient.url=stork://foo-service/graphql\n" +
                                    "quarkus.stork.foo-service.service-discovery.type=static\n" +
                                    "quarkus.stork.foo-service.service-discovery.address-list=${quarkus.http.host:localhost}:${quarkus.http.test-port:8081}"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    TestingGraphQLClientApi client;

    @Test
    public void performQuery() {
        List<Person> people = client.people();
        assertEquals("John", people.get(0).getFirstName());
        assertEquals("Arthur", people.get(1).getFirstName());
    }

}
