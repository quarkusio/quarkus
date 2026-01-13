package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.HttpHeadersResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

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
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(HttpHeadersResource.class);
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

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Client invokes GET request on a sub resource at /HeadersTest/sub2
     *                with Accept MediaType and Content-Type Headers set;
     *                Verify that HttpHeaders got the property set by the request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Request Headers Test")
    public void RequestHeadersTest() {
        Response response = client.target(UriBuilder.fromUri(uri).path("/HeadersTest/headers")).request()
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
