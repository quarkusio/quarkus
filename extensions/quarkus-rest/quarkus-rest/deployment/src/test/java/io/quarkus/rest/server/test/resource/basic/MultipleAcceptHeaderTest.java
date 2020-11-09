package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.reactive.client.QuarkusRestClient;
import org.jboss.resteasy.reactive.client.QuarkusRestWebTarget;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Multiple Accept Header Test")
public class MultipleAcceptHeaderTest {

    protected static String APPLICATION_JSON = "Content-Type: application/json";

    protected static String APPLICATION_XML = "Content-Type: application/xml";

    private TestInterfaceClient service;

    private QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(TestResourceServer.class, PortProviderUtil.class);
                    return war;
                }
            });

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl();
    }

    @Path("/test")
    @DisplayName("Test Resource Server")
    public static class TestResourceServer {

        @GET
        @Path("accept")
        @Produces("application/json")
        public String acceptJson() {
            return APPLICATION_JSON;
        }

        @GET
        @Path("accept")
        @Produces("application/xml, text/plain")
        public String acceptXml() {
            return APPLICATION_XML;
        }
    }

    @Path("test")
    @DisplayName("Test Interface Client")
    interface TestInterfaceClient {

        @GET
        @Path("accept")
        @Produces("application/json")
        String getJson();

        @GET
        @Path("accept")
        @Produces("application/xml")
        String getXml();

        @GET
        @Path("accept")
        @Produces({ "application/wrong1", "application/wrong2", "application/xml" })
        String getXmlMultiple();

        @GET
        @Path("accept")
        @Produces({ "application/wrong1", "text/plain" })
        String getXmlPlainMultiple();
    }

    @BeforeEach
    public void setUp() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateBaseUrl());
        service = target.proxy(TestInterfaceClient.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.close();
        client = null;
    }

    @Test
    @DisplayName("Test Single Accept Header")
    public void testSingleAcceptHeader() throws Exception {
        String result = service.getJson();
        Assertions.assertEquals(APPLICATION_JSON, result);
    }

    @Test
    @DisplayName("Test Single Accept Header 2")
    public void testSingleAcceptHeader2() throws Exception {
        String result = service.getXml();
        Assertions.assertEquals(APPLICATION_XML, result);
    }

    @Test
    @DisplayName("Test Multiple Accept Header")
    public void testMultipleAcceptHeader() throws Exception {
        String result = service.getXmlMultiple();
        Assertions.assertEquals(APPLICATION_XML, result);
    }

    @Test
    @DisplayName("Test Multiple Accept Header Second Header")
    public void testMultipleAcceptHeaderSecondHeader() throws Exception {
        String result = service.getXmlPlainMultiple();
        Assertions.assertEquals(APPLICATION_XML, result);
    }
}
