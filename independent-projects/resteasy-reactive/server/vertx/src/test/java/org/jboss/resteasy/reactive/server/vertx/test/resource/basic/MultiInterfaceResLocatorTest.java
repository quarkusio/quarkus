package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorIntf1;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorIntf2;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorResource;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorSubresource;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Multi Interface Res Locator Test")
public class MultiInterfaceResLocatorTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(MultiInterfaceResLocatorIntf1.class);
                    war.addClass(MultiInterfaceResLocatorIntf2.class);
                    war.addClasses(PortProviderUtil.class, MultiInterfaceResLocatorResource.class,
                            MultiInterfaceResLocatorSubresource.class);
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MultiInterfaceResLocatorTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for resource with more interfaces.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test")
    public void test() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(generateURL("/test/hello1")).request().get();
        String entity = response.readEntity(String.class);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("resourceMethod1", entity, "Wrong content of response");
        response = client.target(generateURL("/test/hello2")).request().get();
        entity = response.readEntity(String.class);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("resourceMethod2", entity, "Wrong content of response");
        client.close();
    }
}
