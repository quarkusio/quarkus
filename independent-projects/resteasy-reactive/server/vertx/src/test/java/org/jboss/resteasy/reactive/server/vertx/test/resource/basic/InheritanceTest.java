package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.InheritenceParentResource;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.InheritenceParentResourceImpl;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests annotation inheritence from interface.
 * @tpSince RESTEasy 3.0.20
 */
@DisplayName("Inheritance Test")
public class InheritanceTest {

    private static Client client;

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(InheritenceParentResource.class);
                    war.addClasses(PortProviderUtil.class, InheritenceParentResourceImpl.class);
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InheritanceTest.class.getSimpleName());
    }

    @BeforeAll
    public static void beforeSub() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void afterSub() {
        client.close();
    }

    @Test
    @DisplayName("Test 1")
    public void Test1() throws Exception {
        Builder builder = client.target(generateURL("/InheritanceTest")).request();
        builder.header("Accept", "text/plain");
        Response response = builder.get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(response.readEntity(String.class), "First");
    }
}
