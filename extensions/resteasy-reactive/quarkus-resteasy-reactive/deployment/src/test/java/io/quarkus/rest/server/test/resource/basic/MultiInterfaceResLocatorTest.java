package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorIntf1;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorIntf2;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorResource;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorSubresource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Multi Interface Res Locator Test")
public class MultiInterfaceResLocatorTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
