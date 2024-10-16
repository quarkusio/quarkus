package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.ClassLevelMediaTypeResource;
import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Class Level Media Type Test")
public class ClassLevelMediaTypeTest {

    private static Client client;
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, ClassLevelMediaTypeResource.class);
                    return war;
                }
            });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
        client = null;
    }

    @Test
    @DisplayName("Test Application Json Media Type")
    public void testApplicationJsonMediaType() {
        WebTarget base = client.target(generateURL("/test"));
        System.err.println(base.getClass());
        try {
            Response response = base.request().get();
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            String body = response.readEntity(String.class);
            Assertions.assertEquals(response.getHeaderString("Content-Type"), "application/json;charset=UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path);
    }
}
