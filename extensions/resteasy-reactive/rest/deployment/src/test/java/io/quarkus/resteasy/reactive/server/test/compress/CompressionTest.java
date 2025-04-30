package io.quarkus.resteasy.reactive.server.test.compress;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class CompressionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MyEndpoint.class)
                    .addAsManifestResource(new StringAsset(MyEndpoint.MESSAGE), "resources/file.txt")
                    .addAsManifestResource(new StringAsset(MyEndpoint.MESSAGE), "resources/my.doc"))
            .overrideConfigKey("quarkus.http.enable-compression", "true");

    @Test
    public void testEndpoint() {
        assertCompressed("/endpoint/compressed");
        assertUncompressed("/endpoint/uncompressed");
        assertCompressed("/endpoint/compressed-content-type");
        assertUncompressed("/endpoint/uncompressed-content-type");
        assertCompressed("/endpoint/content-type-implicitly-compressed");
        assertCompressed("/endpoint/content-type-with-param-implicitly-compressed");
        assertUncompressed("/endpoint/content-type-implicitly-uncompressed");
        assertCompressed("/endpoint/content-type-in-produces-compressed");
        assertUncompressed("/endpoint/content-type-in-produces-uncompressed");

        assertCompressed("/file.txt");
        assertUncompressed("/my.doc");
    }

    private void assertCompressed(String path) {
        String bodyStr = get(path).then().statusCode(200).header("Content-Encoding", "gzip").extract().asString();
        assertEquals(MyEndpoint.MESSAGE, bodyStr);
    }

    private void assertUncompressed(String path) {
        ExtractableResponse<Response> response = get(path)
                .then().statusCode(200).extract();
        assertTrue(response.header("Content-Encoding") == null, response.headers().toString());
        assertEquals(MyEndpoint.MESSAGE, response.asString());
    }

    @Path("endpoint")
    public static class MyEndpoint {

        static String MESSAGE = "Hello compression!";

        @Compressed
        @GET
        @Path("compressed")
        public String compressed() {
            return MESSAGE;
        }

        @Uncompressed
        @GET
        @Path("uncompressed")
        public String uncompressed() {
            return MESSAGE;
        }

        @Uncompressed
        @GET
        @Path("uncompressed-content-type")
        public RestResponse<Object> uncompressedContentType() {
            return RestResponse.ResponseBuilder.ok().entity(MESSAGE).header("Content-type", "text/plain").build();
        }

        @Compressed
        @GET
        @Path("compressed-content-type")
        public RestResponse<Object> compressedContentType() {
            return RestResponse.ResponseBuilder.ok().entity(MESSAGE).header("Content-type", "foo/bar").build();
        }

        @GET
        @Path("content-type-implicitly-compressed")
        public RestResponse<Object> contentTypeImplicitlyCompressed() {
            return RestResponse.ResponseBuilder.ok().entity(MESSAGE).header("Content-type", "text/plain").build();
        }

        @GET
        @Path("content-type-with-param-implicitly-compressed")
        public RestResponse<Object> contentTypeWithParamImplicitlyCompressed() {
            return RestResponse.ResponseBuilder.ok().entity(MESSAGE).header("Content-type", "text/plain;charset=UTF-8").build();
        }

        @GET
        @Path("content-type-implicitly-uncompressed")
        public RestResponse<Object> contentTypeImplicitlyUncompressed() {
            return RestResponse.ResponseBuilder.ok().entity(MESSAGE).header("Content-type", "foo/bar").build();
        }

        // uses 'text/plain' as the default type
        @GET
        @Path("content-type-in-produces-compressed")
        public String contentTypeInProducesCompressed() {
            return MESSAGE;
        }

        @Produces("foo/bar")
        @GET
        @Path("content-type-in-produces-uncompressed")
        public String contentTypeInProducesUncompressed() {
            return MESSAGE;
        }
    }

}
