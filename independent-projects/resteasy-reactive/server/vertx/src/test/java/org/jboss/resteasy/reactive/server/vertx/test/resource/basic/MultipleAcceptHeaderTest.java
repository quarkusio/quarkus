package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientBuilder;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@DisplayName("Multiple Accept Header Test")
@Disabled("needs client proxies")
public class MultipleAcceptHeaderTest {

    protected static String APPLICATION_JSON = "Content-Type: application/json";

    protected static String APPLICATION_XML = "Content-Type: application/xml";

    private TestInterfaceClient service;

    private ClientImpl client;

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
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
        client = (ClientImpl) ClientBuilder.newClient();
        WebTargetImpl target = (WebTargetImpl) client.target(generateBaseUrl());
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
