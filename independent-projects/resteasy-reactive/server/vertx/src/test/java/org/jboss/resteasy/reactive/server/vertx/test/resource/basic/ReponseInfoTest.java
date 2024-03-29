package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ResponseInfoResource;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Reponse Info Test")
public class ReponseInfoTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, ReponseInfoTest.class);
                    // Use of PortProviderUtil in the deployment
                    war.addClasses(ResponseInfoResource.class);
                    return war;
                }
            });

    private void basicTest(String path) {
        WebTarget base = client.target(PortProviderUtil.generateURL(path, ReponseInfoTest.class.getSimpleName()));
        Response response = base.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(true, response.readEntity(boolean.class));
        response.close();
    }

    /**
     * @tpTestDetails Check URI location from HTTP headers from response prepared in resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Info")
    public void testUriInfo() throws Exception {
        basicTest("/simple");
    }
}
