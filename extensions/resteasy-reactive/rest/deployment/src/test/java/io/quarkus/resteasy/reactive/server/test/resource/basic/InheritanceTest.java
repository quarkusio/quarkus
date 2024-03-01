package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.InheritanceParentResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.InheritanceParentResourceImpl;
import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
                    war.addClasses(PortProviderUtil.class, InheritanceParentResourceImpl.class);
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
