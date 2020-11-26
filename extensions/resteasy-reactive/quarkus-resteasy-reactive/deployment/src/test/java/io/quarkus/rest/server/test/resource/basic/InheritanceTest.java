package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.InheritenceParentResource;
import io.quarkus.rest.server.test.resource.basic.resource.InheritenceParentResourceImpl;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
