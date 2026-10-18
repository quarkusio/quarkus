package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class FileInputWithDeleteAndInterceptorTest extends AbstractMultipartTest {

    private static final java.nio.file.Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(Resource.class, NoopReaderInterceptor.class))
            .overrideConfigKey("quarkus.http.body.uploads-directory", uploadDir.toString());

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File HTML_FILE2 = new File("./src/test/resources/test2.html");

    @Test
    public void test() throws IOException {
        RestAssured.given()
                .contentType("application/octet-stream")
                .body(HTML_FILE)
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(equalTo(fileSizeAsStr(HTML_FILE)));

        awaitUploadDirectoryToEmpty(uploadDir);

        RestAssured.given()
                .contentType("application/octet-stream")
                .body(HTML_FILE2)
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(equalTo(fileSizeAsStr(HTML_FILE2)));

        awaitUploadDirectoryToEmpty(uploadDir);
    }

    @Path("test")
    public static class Resource {

        @POST
        @Consumes("application/octet-stream")
        public long size(File file) throws IOException {
            return Files.size(file.toPath());
        }
    }

    @Provider
    public static class NoopReaderInterceptor implements ReaderInterceptor {
        @Override
        public Object aroundReadFrom(ReaderInterceptorContext ctx)
                throws IOException, WebApplicationException {
            return ctx.proceed();
        }
    }
}
