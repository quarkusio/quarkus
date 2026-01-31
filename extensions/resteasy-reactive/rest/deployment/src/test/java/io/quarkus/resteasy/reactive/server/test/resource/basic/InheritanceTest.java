package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.InheritanceAbstractParentImplResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.InheritanceAbstractParentResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.InheritanceParentResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.InheritanceParentResourceImpl;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests annotation inheritance from interface.
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("Inheritance Test")
public class InheritanceTest {

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(InheritanceParentResource.class);
                    war.addClass(InheritanceAbstractParentResource.class);
                    war.addClass(InheritanceAbstractParentImplResource.class);
                    war.addClasses(InheritanceParentResourceImpl.class);
                    return war;
                }
            });

    @BeforeAll
    public static void beforeSub() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void afterSub() {
        client.close();
    }

    @TestHTTPResource
    URI uri;

    @Test
    @DisplayName("Test 1")
    public void Test1() {
        Builder builder = client.target(UriBuilder.fromUri(uri).path("/InheritanceTest")).request();
        builder.header("Accept", "text/plain");
        Response response = builder.get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("First", response.readEntity(String.class));
    }

    @Test
    public void testAbstractParent() {
        Builder builder = client.target(UriBuilder.fromUri(uri).path("/inheritance-abstract-parent-test")).request();
        builder.header("Accept", "text/plain");
        Response response = builder.get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("works", response.readEntity(String.class));
    }
}
