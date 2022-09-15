package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.CovariantReturnSubresourceLocatorsRootProxy;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxy;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxyRootImpl;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxySubImpl;
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
 * @tpTestCaseDetails Test return value of covariant with locators.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Covariant Return Subresource Locators Test")
public class CovariantReturnSubresourceLocatorsTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
                    jar.addClasses(CovariantReturnSubresourceLocatorsRootProxy.class, PortProviderUtil.class,
                            CovariantReturnSubresourceLocatorsSubProxy.class);
                    jar.addClasses(CovariantReturnSubresourceLocatorsSubProxyRootImpl.class,
                            CovariantReturnSubresourceLocatorsSubProxySubImpl.class);
                    return jar;
                }
            });

    /**
     * @tpTestDetails Test basic path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Basic Test")
    public void basicTest() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(
                PortProviderUtil.generateURL("/path/sub/xyz", CovariantReturnSubresourceLocatorsTest.class.getSimpleName()))
                .request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("Boo! - xyz", response.readEntity(String.class), "Wrong content of response");
        response.close();
        client.close();
    }
}
