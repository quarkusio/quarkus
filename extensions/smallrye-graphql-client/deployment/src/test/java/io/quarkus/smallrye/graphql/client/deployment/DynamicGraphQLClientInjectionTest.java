package io.quarkus.smallrye.graphql.client.deployment;

import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
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
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.NamedClient;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.core.Operation;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public class DynamicGraphQLClientInjectionTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestingGraphQLApi.class, Person.class)
                    .addAsResource(new StringAsset("people/mp-graphql/url=" + url),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    @NamedClient("people")
    DynamicGraphQLClient client;

    @Test
    public void checkInjectedClient() throws NoSuchFieldException, IllegalAccessException {
        Document query = document(
                Operation.operation("PeopleQuery", field("people", field("firstName"), field("lastName"))));
        List<Person> people = client.executeAsync(query)
                .await().atMost(Duration.ofSeconds(30)).getList(Person.class, "people");
        assertEquals("John", people.get(0).getFirstName());
        assertEquals("Arthur", people.get(1).getFirstName());
    }

}
