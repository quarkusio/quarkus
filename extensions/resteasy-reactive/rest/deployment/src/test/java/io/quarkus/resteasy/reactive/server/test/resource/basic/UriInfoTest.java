package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.GetAbsolutePathResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.UriInfoEncodedQueryResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.UriInfoEncodedTemplateResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.UriInfoEscapedMatrParamResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.UriInfoQueryParamsResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.UriInfoSimpleResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.UriInfoSimpleSingletonResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for java.net.URI class
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Uri Info Test")
public class UriInfoTest {

    protected final Logger logger = Logger.getLogger(UriInfoTest.class);

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest quarkusUnitTest = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(UriInfoSimpleResource.class, UriInfoEncodedQueryResource.class,
                            UriInfoQueryParamsResource.class, UriInfoSimpleSingletonResource.class,
                            UriInfoEncodedTemplateResource.class, UriInfoEscapedMatrParamResource.class,
                            UriInfoEncodedTemplateResource.class, GetAbsolutePathResource.class);
                    return war;
                }
            });

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
        client = null;
    }

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Check uri from resource on server. Simple resource is used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Info")
    public void testUriInfo() {
        try (Response response1 = client.target(UriBuilder.fromUri(uri)
                .path(UriInfoSimpleResource.class.getSimpleName())
                .path("/simple"))
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response1.getStatus());
        }
        try (Response response = client.target(UriBuilder.fromUri(uri)
                .path(UriInfoSimpleResource.class.getSimpleName())
                .path("/simple/fromField"))
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }

        RestAssured.get("/" + UriInfoSimpleResource.class.getSimpleName() + "/uri?foo=bar").then()
                .body(Matchers.endsWith("/uri?foo=bar"));
    }

    /**
     * @tpTestDetails Check uri from resource on server. Resource is set as singleton to RESTEasy.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Disabled
    @DisplayName("Test Uri Info With Singleton")
    public void testUriInfoWithSingleton() {
        try (Response response = client.target(UriBuilder.fromUri(uri)
                .path(UriInfoSimpleSingletonResource.class.getSimpleName())
                .path("/simple/fromField"))
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @tpTestDetails Check uri from resource on server. Test complex parameter.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Disabled
    @DisplayName("Test Escaped Matr Param")
    public void testEscapedMatrParam() {
        try (Response response = client.target(UriBuilder.fromUri(uri)
                .path(UriInfoEscapedMatrParamResource.class.getSimpleName())
                .path("/queryEscapedMatrParam;a=a%3Bb;b=x%2Fy;c=m%5Cn;d=k%3Dl"))
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @tpTestDetails Check uri from resource on server. Test space character in URI.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Disabled
    @DisplayName("Test Encoded Template Params")
    public void testEncodedTemplateParams() {
        try (Response response = client.target(UriBuilder.fromUri(uri)
                .path(UriInfoEncodedTemplateResource.class.getSimpleName())
                .path("/a%20b/x%20y"))
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @tpTestDetails Check uri from resource on server. Test space character in URI attribute.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Disabled
    @DisplayName("Test Encoded Query Params")
    public void testEncodedQueryParams() {
        try (Response response = client.target(UriBuilder.fromUri(uri)
                .path(UriInfoEncodedQueryResource.class.getSimpleName())
                .path("/query"))
                .queryParam("a", "a%20b")
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @tpTestDetails Check uri from resource on server. Test return value from resource - same URI address.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @Disabled
    @DisplayName("Test Relativize")
    public void testRelativize() {
        WebTarget target = client.target(uri);
        String result;
        result = target.path("a/b/c").queryParam("to", "a/d/e").request().get(String.class);
        assertEquals("../../d/e", result);
        result = target.path("a/b/c").queryParam("to", UriBuilder.fromUri(uri).path("a/d/e").build().toString()).request()
                .get(String.class);
        assertEquals("../../d/e", result);
        result = target.path("a/b/c").queryParam("to", "http://foobar/a/d/e").request().get(String.class);
        assertEquals("http://foobar/a/d/e", result);
    }

    /**
     * @tpTestDetails Test that UriInfo.getQueryParameters() returns an immutable map. Test's logic is in end-point.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Query Params Mutability")
    public void testQueryParamsMutability() {
        try (Response response = client.target(UriBuilder.fromUri(uri)
                .path("UriInfoQueryParamsResource")
                .path("/queryParams")
                .queryParam("a", "a,b"))
                .request().get()) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    @DisplayName("Test Get Absolute Path")
    public void testGetAbsolutePath() {
        doTestGetAbsolutePath("/absolutePath", "unset");
        doTestGetAbsolutePath("/absolutePath?dummy=1234", "1234");
        doTestGetAbsolutePath("/absolutePath?foo=bar&dummy=1234", "1234");
    }

    private void doTestGetAbsolutePath(String path, String expectedDummyHeader) {
        String absolutePathHeader = RestAssured.get(path)
                .then()
                .statusCode(200)
                .header("dummy", expectedDummyHeader).extract().header("absolutePath");
        org.assertj.core.api.Assertions.assertThat(absolutePathHeader).endsWith("/absolutePath");
    }
}
