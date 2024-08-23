package io.quarkus.vertx.web.compress;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;
import io.quarkus.vertx.web.Route;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.ext.web.RoutingContext;

public class CompressionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MyRoutes.class)
                    .addAsManifestResource(new StringAsset(MyRoutes.MESSAGE), "resources/file.txt")
                    .addAsManifestResource(new StringAsset(MyRoutes.MESSAGE), "resources/my.doc"))
            .overrideConfigKey("quarkus.http.enable-compression", "true");

    @Test
    public void testRoutes() {
        assertCompressed("/compressed");
        assertUncompressed("/uncompressed");
        assertCompressed("/compressed-content-type");
        assertUncompressed("/uncompressed-content-type");
        assertCompressed("/content-type-implicitly-compressed");
        assertCompressed("/content-type-with-param-implicitly-compressed");
        assertUncompressed("/content-type-implicitly-uncompressed");
        assertCompressed("/compression-disabled-manually");
        assertCompressed("/file.txt");
        assertUncompressed("/my.doc");
    }

    private void assertCompressed(String path) {
        String bodyStr = get(path).then().statusCode(200).header("Content-Encoding", "gzip").extract().asString();
        assertEquals("Hello compression!", bodyStr);
    }

    private void assertUncompressed(String path) {
        ExtractableResponse<Response> response = get(path)
                .then().statusCode(200).extract();
        assertTrue(response.header("Content-Encoding") == null, response.headers().toString());
        assertEquals(MyRoutes.MESSAGE, response.asString());
    }

    @ApplicationScoped
    public static class MyRoutes {

        static String MESSAGE = "Hello compression!";

        @Compressed
        @Route
        String compressed() {
            return MESSAGE;
        }

        @Uncompressed
        @Route
        String uncompressed() {
            return MESSAGE;
        }

        @Uncompressed
        @Route
        void uncompressedContentType(RoutingContext context) {
            context.response().setStatusCode(200).putHeader("Content-type", "text/plain").end(MESSAGE);
        }

        @Compressed
        @Route
        void compressedContentType(RoutingContext context) {
            context.response().setStatusCode(200).putHeader("Content-type", "foo/bar").end(MESSAGE);
        }

        @Route
        void contentTypeImplicitlyCompressed(RoutingContext context) {
            context.response().setStatusCode(200).putHeader("Content-type", "text/plain").end(MESSAGE);
        }

        @Route
        void contentTypeWithParamImplicitlyCompressed(RoutingContext context) {
            context.response().setStatusCode(200).putHeader("Content-type", "text/plain;charset=UTF-8").end(MESSAGE);
        }

        @Route
        void contentTypeImplicitlyUncompressed(RoutingContext context) {
            context.response().setStatusCode(200).putHeader("Content-type", "foo/bar").end(MESSAGE);
        }

        @Route
        void compressionDisabledManually(RoutingContext context) {
            context.response().headers().remove("Content-Encoding");
            context.response().setStatusCode(200).putHeader("Content-type", "text/plain").end(MESSAGE);
        }

    }

}
