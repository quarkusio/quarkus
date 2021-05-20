package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.URL;

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
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.graphql.client.NamedClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

public class DynamicGraphQLClientInjectionTest {

    @TestHTTPResource
    static URL url;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestingGraphQLApi.class, Person.class)
                    .addAsResource(new StringAsset("people/mp-graphql/url=http://example.org/graphql"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    @NamedClient("people")
    DynamicGraphQLClient client;

    @Test
    public void checkInjectedClient() throws NoSuchFieldException, IllegalAccessException {
        // In this test, we only check that the injected client instance has the correct URL value
        // I don't know if it's possible to know the actual URL (where the server side
        // will be available) beforehand, so that we could put it into application.properties and then actually
        // use the injected client to call something on the server
        assertNotNull(client);
        Field urlField = client.getClass().getDeclaredField("url");
        urlField.setAccessible(true);
        assertEquals("http://example.org/graphql", urlField.get(client));
    }

}
