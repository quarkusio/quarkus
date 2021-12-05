package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.client.deployment.model.Person;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLApi;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLClientApiWithNoConfigKey;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that it's possible to configure a typesafe client using the short class name, the
 * package will be deduced.
 */
public class TypesafeGraphQLClientShortcutTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestingGraphQLApi.class, TestingGraphQLClientApiWithNoConfigKey.class, Person.class)
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql-client.TestingGraphQLClientApiWithNoConfigKey.url=" + url),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    TestingGraphQLClientApiWithNoConfigKey client;

    @Test
    public void performQuery() {
        List<Person> people = client.people();
        assertEquals("John", people.get(0).getFirstName());
        assertEquals("Arthur", people.get(1).getFirstName());
    }

}
