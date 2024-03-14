package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;

public class MultipartTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FormData.class, Person.class, Views.class, MultipartResource.class)
                            .addAsResource(new StringAsset("quarkus.http.body.delete-uploaded-files-on-end=true\n"),
                                    "application.properties");
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");

    @Test
    public void testValid() throws IOException {
        testValid("/multipart/json");
    }

    private void testValid(String url) throws IOException {
        // NOTE: the multipart file name is ignored for String types, so we must convert them
        // to byte[] in order to send a file name
        RestAssured.given()
                .multiPart("map",
                        "{\n" +
                                "  \"foo\": \"bar\",\n" +
                                "  \"sub\": {\n" +
                                "    \"foo2\": \"bar2\"\n" +
                                "  }\n" +
                                "}")
                .multiPart(new MultiPartSpecBuilder(("{\n" +
                        "  \"foo2\": \"bar\",\n" +
                        "  \"sub2\": {\n" +
                        "    \"foo2\": \"bar2\"\n" +
                        "  }\n" +
                        "}").getBytes()).controlName("map2").fileName("something.js").build())
                .multiPart("person", "{\"first\": \"Bob\", \"last\": \"Builder\"}", "application/json")
                .multiPart(new MultiPartSpecBuilder("{\"first\": \"Bob\", \"last\": \"Builder\"}".getBytes())
                        .controlName("person2").fileName("something.js").mimeType("application/json").build())
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("names", "name1")
                .multiPart("names", "name2")
                .multiPart("numbers", 1)
                .multiPart("numbers", 2)
                .multiPart("numbers2", 1)
                .multiPart("numbers2", 2)
                .multiPart("persons", "{\"first\": \"First1\", \"last\": \"Last1\"}", "application/json")
                .multiPart("persons", "{\"first\": \"First2\", \"last\": \"Last2\"}", "application/json")
                .multiPart("persons2", "{\"first\": \"First1\", \"last\": \"Last1\"}", "application/json")
                .multiPart("persons2", "{\"first\": \"First2\", \"last\": \"Last2\"}", "application/json")
                .multiPart(new MultiPartSpecBuilder("{\"first\": \"First1\", \"last\": \"Last1\"}".getBytes())
                        .controlName("persons3").fileName("something.js").mimeType("application/json").build())
                .multiPart(new MultiPartSpecBuilder("{\"first\": \"First2\", \"last\": \"Last2\"}".getBytes())
                        .controlName("persons3").fileName("something.js").mimeType("application/json").build())
                .multiPart(new MultiPartSpecBuilder("{\"first\": \"First1\", \"last\": \"Last1\"}".getBytes())
                        .controlName("persons4").fileName("something.js").mimeType("application/json").build())
                .multiPart(new MultiPartSpecBuilder("{\"first\": \"First2\", \"last\": \"Last2\"}".getBytes())
                        .controlName("persons4").fileName("something.js").mimeType("application/json").build())
                .accept("application/json")
                .when()
                .post(url)
                .then()
                .statusCode(200)
                .body("foo", equalTo("bar"))
                .body("sub.foo2", equalTo("bar2"))
                .body("foo2", equalTo("bar"))
                .body("sub2.foo2", equalTo("bar2"))
                .body("person.first", equalTo("Bob"))
                .body("person.last", equalTo("Builder"))
                .body("person2.first", equalTo("Bob"))
                .body("person2.last", equalTo("Builder"))
                .body("htmlFileSize", equalTo(Files.readAllBytes(HTML_FILE.toPath()).length))
                .body("htmlFilePath", not(equalTo(HTML_FILE.toPath().toAbsolutePath().toString())))
                .body("htmlFileContentType", equalTo("text/html"))
                .body("names[0]", equalTo("name1"))
                .body("names[1]", equalTo("name2"))
                .body("numbers[0]", equalTo(1))
                .body("numbers[1]", equalTo(2))
                .body("numbers2[0]", equalTo(1))
                .body("numbers2[1]", equalTo(2))
                .body("persons[0].first", equalTo("First1"))
                .body("persons[0].last", equalTo("Last1"))
                .body("persons[1].first", equalTo("First2"))
                .body("persons[1].last", equalTo("Last2"))
                .body("persons2[0].first", equalTo("First1"))
                .body("persons2[0].last", equalTo("Last1"))
                .body("persons2[1].first", equalTo("First2"))
                .body("persons2[1].last", equalTo("Last2"))
                .body("persons3[0].first", equalTo("First1"))
                .body("persons3[0].last", equalTo("Last1"))
                .body("persons3[1].first", equalTo("First2"))
                .body("persons3[1].last", equalTo("Last2"))
                .body("persons4[0].first", equalTo("First1"))
                .body("persons4[0].last", equalTo("Last1"))
                .body("persons4[1].first", equalTo("First2"))
                .body("persons4[1].last", equalTo("Last2"));
    }

    @Test
    public void testValidParam() throws IOException {
        testValid("/multipart/param/json");
    }

    @Test
    public void testInvalid() {
        RestAssured.given()
                .multiPart("map",
                        "{\n" +
                                "  \"foo\": \"bar\",\n" +
                                "  \"sub\": {\n" +
                                "    \"foo2\": \"bar2\"\n" +
                                "  }\n" +
                                "}")
                .multiPart("person", "{\"first\": \"Bob\"}", "application/json")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("names", "name1")
                .multiPart("names", "name2")
                .multiPart("numbers", 1)
                .multiPart("numbers", 2)
                .accept("application/json")
                .when()
                .post("/multipart/json")
                .then()
                .statusCode(400);
    }

    @Test
    public void testInvalidParam() {
        RestAssured.given()
                .multiPart("map",
                        "{\n" +
                                "  \"foo\": \"bar\",\n" +
                                "  \"sub\": {\n" +
                                "    \"foo2\": \"bar2\"\n" +
                                "  }\n" +
                                "}")
                .multiPart("person", "{\"first\": \"Bob\"}", "application/json")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("names", "name1")
                .multiPart("names", "name2")
                .multiPart("numbers", 1)
                .multiPart("numbers", 2)
                .accept("application/json")
                .when()
                .post("/multipart/param/json")
                .then()
                .statusCode(400);
    }
}
