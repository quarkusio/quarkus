package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.ResponseInfoResource;
import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
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
