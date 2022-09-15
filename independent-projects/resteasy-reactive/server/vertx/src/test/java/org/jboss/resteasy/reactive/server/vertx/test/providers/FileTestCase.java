package org.jboss.resteasy.reactive.server.vertx.test.providers;

import io.restassured.RestAssured;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.FilePart;
import org.jboss.resteasy.reactive.PathPart;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FileTestCase {

    private static final String FILE = "src/test/resources/lorem.txt";

    @RegisterExtension
    static final ResteasyReactiveUnitTest config = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FileResource.class, WithWriterInterceptor.class, WriterInterceptor.class));

    @Test
    public void testFiles() throws Exception {

        String content = Files.readString(Path.of(FILE));
        String contentLength = String.valueOf(content.length());
        RestAssured.get("/providers/file/file")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                .body(Matchers.equalTo(content));
        RestAssured.get("/providers/file/file-partial")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, "10")
                .body(Matchers.equalTo(content.substring(20, 30)));
        RestAssured.get("/providers/file/path")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                .body(Matchers.equalTo(content));
        RestAssured.get("/providers/file/path-partial")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, "10")
                .body(Matchers.equalTo(content.substring(20, 30)));
        RestAssured.get("/providers/file/async-file")
                .then()
                .header(HttpHeaders.CONTENT_LENGTH, Matchers.nullValue())
                .statusCode(200)
                .body(Matchers.equalTo(content));
        RestAssured.get("/providers/file/mutiny-async-file")
                .then()
                .header(HttpHeaders.CONTENT_LENGTH, Matchers.nullValue())
                .statusCode(200)
                .body(Matchers.equalTo(content));
        RestAssured.get("/providers/file/async-file-partial")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, "10")
                .body(Matchers.equalTo(content.substring(20, 30)));
    }

    @Test
    public void testChecks() throws IOException {
        // creation-time checks
        Path path = Paths.get(FILE);
        // works
        new PathPart(path, 10, 10);
        new PathPart(path, 0, Files.size(path));
        // fails
        try {
            new PathPart(path, -1, 10);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
        try {
            new PathPart(path, 0, -1);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
        try {
            new PathPart(path, 0, 1000);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
        try {
            new PathPart(path, 250, 250);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }

        File file = new File(FILE);
        // works
        new FilePart(file, 10, 10);
        new FilePart(file, 0, file.length());
        // fails
        try {
            new FilePart(file, -1, 10);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
        try {
            new FilePart(file, 0, -1);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
        try {
            new FilePart(file, 0, 1000);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
        try {
            new FilePart(file, 250, 250);
            Assertions.fail();
        } catch (IllegalArgumentException x) {
        }
    }
}
