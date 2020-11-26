package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.HttpHeadersResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Spec requires that HEAD and OPTIONS are handled in a default manner
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Http Headers Test")
public class HttpHeadersTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, HttpHeadersResource.class);
                    return war;
                }
            });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, HttpHeadersTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client invokes GET request on a sub resource at /HeadersTest/sub2
     *                with Accept MediaType and Content-Type Headers set;
     *                Verify that HttpHeaders got the property set by the request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Request Headers Test")
    public void RequestHeadersTest() throws Exception {
        String errorMessage = "Wrong content of response";
        Response response = client.target(generateURL("/HeadersTest/headers")).request()
                .header("Accept", "text/plain, text/html, text/html;level=1, */*")
                .header("Content-Type", "application/xml;charset=utf8").get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String content = response.readEntity(String.class);
        Assertions.assertTrue(-1 < content.indexOf("Accept:"));
        Assertions.assertTrue(-1 < content.indexOf("Content-Type:"));
        Assertions.assertTrue(-1 < content.indexOf("application/xml"));
        Assertions.assertTrue(-1 < content.indexOf("charset=utf8"));
        Assertions.assertTrue(-1 < content.indexOf("text/html"));
        Assertions.assertTrue(-1 < content.indexOf("*/*"));
        response.close();
    }
}
