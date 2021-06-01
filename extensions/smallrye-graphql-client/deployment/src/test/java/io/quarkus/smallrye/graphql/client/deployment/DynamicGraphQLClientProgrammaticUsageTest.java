package io.quarkus.smallrye.graphql.client.deployment;

import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.client.deployment.model.Person;
import io.quarkus.smallrye.graphql.client.deployment.model.TestingGraphQLApi;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.core.Operation;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

public class DynamicGraphQLClientProgrammaticUsageTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestingGraphQLApi.class, Person.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @TestHTTPResource
    URL url;

    @Test
    public void performCallSync() throws Exception {
        try (DynamicGraphQLClient client = DynamicGraphQLClientBuilder.newBuilder().url(url.toString() + "/graphql").build()) {
            Document query = document(
                    Operation.operation("PeopleQuery", field("people", field("firstName"), field("lastName"))));
            List<Person> people = client.executeSync(query).getList(Person.class, "people");
            assertEquals("John", people.get(0).getFirstName());
            assertEquals("Arthur", people.get(1).getFirstName());
        }
    }

    @Test
    public void performCallAsync() throws Exception {
        try (DynamicGraphQLClient client = DynamicGraphQLClientBuilder.newBuilder().url(url.toString() + "/graphql").build()) {
            Document query = document(
                    Operation.operation("PeopleQuery", field("people", field("firstName"), field("lastName"))));
            List<Person> people = client.executeAsync(query)
                    .await().atMost(Duration.ofSeconds(30)).getList(Person.class, "people");
            assertEquals("John", people.get(0).getFirstName());
            assertEquals("Arthur", people.get(1).getFirstName());
        }
    }

}
