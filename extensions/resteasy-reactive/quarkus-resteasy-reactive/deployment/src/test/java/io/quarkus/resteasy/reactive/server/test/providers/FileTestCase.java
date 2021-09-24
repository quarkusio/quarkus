package io.quarkus.resteasy.reactive.server.test.providers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.FilePart;
import org.jboss.resteasy.reactive.PathPart;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class FileTestCase {

    private final static String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut\n"
            +
            "enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor\n"
            +
            "in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident,\n"
            +
            " sunt in culpa qui officia deserunt mollit anim id est laborum.\n" +
            "\n" +
            "";
    private static final String FILE = "src/test/resources/lorem.txt";

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FileResource.class, WithWriterInterceptor.class, WriterInterceptor.class));

    @Test
    public void testFiles() throws Exception {
        // adjusting expected file size for Windows, whose git checkout will adjust line separators
        String content;
        if (System.lineSeparator().length() == 2) {
            content = LOREM.replace("\n", System.lineSeparator());
        } else {
            content = LOREM;
        }
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
                .body(Matchers.equalTo(LOREM.substring(20, 30)));
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
