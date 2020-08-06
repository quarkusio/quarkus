package io.quarkus.qrs.test.resource.basic;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Multiple Accept Header Test")
public class MultipleAcceptHeaderTest {
    //
    //    protected static String APPLICATION_JSON = "Content-Type: application/json";
    //
    //    protected static String APPLICATION_XML = "Content-Type: application/xml";
    //
    //    private TestInterfaceClient service;
    //
    //    private Client client;
    //
    //    @RegisterExtension
    //    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
    //            .setArchiveProducer(new Supplier<JavaArchive>() {
    //                @Override
    //                public JavaArchive get() {
    //                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
    //                    war.addClasses(TestResourceServer.class);
    //                    return war;
    //                }
    //            });
    //
    //    private String generateBaseUrl() {
    //        return PortProviderUtil.generateBaseUrl();
    //    }
    //
    //    @Path("/test")
    //    @DisplayName("Test Resource Server")
    //    public static class TestResourceServer {
    //
    //        @GET
    //        @Path("accept")
    //        @Produces("application/json")
    //        public String acceptJson() {
    //            return APPLICATION_JSON;
    //        }
    //
    //        @GET
    //        @Path("accept")
    //        @Produces({ "application/xml", "text/plain" })
    //        public String acceptXml() {
    //            return APPLICATION_XML;
    //        }
    //    }
    //
    //    @Path("test")
    //    @DisplayName("Test Interface Client")
    //    interface TestInterfaceClient {
    //
    //        @GET
    //        @Path("accept")
    //        @Produces("application/json")
    //        String getJson();
    //
    //        @GET
    //        @Path("accept")
    //        @Produces("application/xml")
    //        String getXml();
    //
    //        @GET
    //        @Path("accept")
    //        @Produces({ "application/wrong1", "application/wrong2", "application/xml" })
    //        String getXmlMultiple();
    //
    //        @GET
    //        @Path("accept")
    //        @Produces({ "application/wrong1", "text/plain" })
    //        String getXmlPlainMultiple();
    //    }
    //
    //    @BeforeEach
    //    public void setUp() throws Exception {
    //        client = ClientBuilder.newClient();
    //        ResteasyWebTarget target = (ResteasyWebTarget) client.target(generateBaseUrl());
    //        service = target.proxy(TestInterfaceClient.class);
    //    }
    //
    //    @AfterEach
    //    public void tearDown() throws Exception {
    //        client.close();
    //        client = null;
    //    }
    //
    //    @Test
    //    @DisplayName("Test Single Accept Header")
    //    public void testSingleAcceptHeader() throws Exception {
    //        String result = service.getJson();
    //        Assertions.assertEquals(APPLICATION_JSON, result);
    //    }
    //
    //    @Test
    //    @DisplayName("Test Single Accept Header 2")
    //    public void testSingleAcceptHeader2() throws Exception {
    //        String result = service.getXml();
    //        Assertions.assertEquals(APPLICATION_XML, result);
    //    }
    //
    //    @Test
    //    @DisplayName("Test Multiple Accept Header")
    //    public void testMultipleAcceptHeader() throws Exception {
    //        String result = service.getXmlMultiple();
    //        Assertions.assertEquals(APPLICATION_XML, result);
    //    }
    //
    //    @Test
    //    @DisplayName("Test Multiple Accept Header Second Header")
    //    public void testMultipleAcceptHeaderSecondHeader() throws Exception {
    //        String result = service.getXmlPlainMultiple();
    //        Assertions.assertEquals(APPLICATION_XML, result);
    //    }
}
