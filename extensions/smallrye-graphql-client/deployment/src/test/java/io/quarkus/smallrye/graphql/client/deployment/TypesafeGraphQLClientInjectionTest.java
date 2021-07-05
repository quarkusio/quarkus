package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.client.deployment.model.Person;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLApi;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLClientApi;
import io.quarkus.test.QuarkusUnitTest;

public class TypesafeGraphQLClientInjectionTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestingGraphQLApi.class, TestingGraphQLClientApi.class, Person.class)
                    .addAsResource(new StringAsset("typesafeclient/mp-graphql/url=" + url + "\n" +
                            "typesafeclient/mp-graphql/header/My-Header=My-Value"),
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

    /**
     * Verify that configured HTTP headers are applied by the client.
     * We do this by asking the server side to read the header received from the client and send
     * its value back to the client.
     */
    @Test
    public void checkHeaders() {
        assertEquals("My-Value", client.returnHeader("My-Header"));
    }

}
