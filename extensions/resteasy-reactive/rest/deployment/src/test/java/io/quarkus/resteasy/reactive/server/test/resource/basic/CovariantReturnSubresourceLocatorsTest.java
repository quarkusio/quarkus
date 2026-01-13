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

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsRootProxy;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxy;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxyRootImpl;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.CovariantReturnSubresourceLocatorsSubProxySubImpl;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

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
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(CovariantReturnSubresourceLocatorsRootProxy.class,
                            CovariantReturnSubresourceLocatorsSubProxy.class);
                    war.addClasses(CovariantReturnSubresourceLocatorsSubProxyRootImpl.class,
                            CovariantReturnSubresourceLocatorsSubProxySubImpl.class);
                    return war;
                }
            });

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Test basic path
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Basic Test")
    public void basicTest() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(UriBuilder.fromUri(uri).path("/path/sub/xyz")).request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("Boo! - xyz", response.readEntity(String.class), "Wrong content of response");
        response.close();
        client.close();
    }
}
