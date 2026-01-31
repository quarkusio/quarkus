package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.MultiInterfaceResLocatorIntf1;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.MultiInterfaceResLocatorIntf2;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.MultiInterfaceResLocatorResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.MultiInterfaceResLocatorSubresource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Multi Interface Res Locator Test")
public class MultiInterfaceResLocatorTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(MultiInterfaceResLocatorIntf1.class);
                    war.addClass(MultiInterfaceResLocatorIntf2.class);
                    war.addClasses(MultiInterfaceResLocatorResource.class, MultiInterfaceResLocatorSubresource.class);
                    return war;
                }
            });

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Test for resource with more interfaces.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test")
    public void test() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(UriBuilder.fromUri(uri).path("/test/hello1")).request().get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("resourceMethod1", entity, "Wrong content of response");
        response = client.target(UriBuilder.fromUri(uri).path("/test/hello2")).request().get();
        entity = response.readEntity(String.class);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("resourceMethod2", entity, "Wrong content of response");
        client.close();
    }
}
