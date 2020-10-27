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

import io.quarkus.rest.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsRootProxy;
import io.quarkus.rest.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxy;
import io.quarkus.rest.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxyRootImpl;
import io.quarkus.rest.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxySubImpl;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test return value of covariant with locators.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Covariant Return Subresource Locators Test")
public class CovariantReturnSubresourceLocatorsTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(CovariantReturnSubresourceLocatorsRootProxy.class, PortProviderUtil.class,
                            CovariantReturnSubresourceLocatorsSubProxy.class);
                    war.addClasses(CovariantReturnSubresourceLocatorsSubProxyRootImpl.class,
                            CovariantReturnSubresourceLocatorsSubProxySubImpl.class);
                    return war;
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
