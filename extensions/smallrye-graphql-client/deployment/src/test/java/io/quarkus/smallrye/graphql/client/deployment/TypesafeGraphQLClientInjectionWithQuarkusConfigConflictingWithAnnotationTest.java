package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.client.deployment.model.Person;
import io.quarkus.smallrye.graphql.client.deployment.model.PersonDto;
import io.quarkus.smallrye.graphql.client.deployment.model.Testing2GraphQLClientApi;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLApi;
import io.quarkus.test.QuarkusUnitTest;

public class TypesafeGraphQLClientInjectionWithQuarkusConfigConflictingWithAnnotationTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestingGraphQLApi.class, Testing2GraphQLClientApi.class,
                            Person.class, PersonDto.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql-client.secondtypesafeclient.url=" + url + "\n"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    Testing2GraphQLClientApi client;

    /**
     * Verify that configured endpoint in 'application.properties' has precedence over the endpoint configured
     * in @GraphQLClientApi of Testing2GraphQLClientApi.
     */
    @Test
    public void performQueryWithSecondClient() {
        List<Person> people = client.people();
        assertEquals("John", people.get(0).getFirstName());
        assertEquals("Arthur", people.get(1).getFirstName());
    }
}
