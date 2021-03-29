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

public class MultipartTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FormData.class, Person.class, MultipartResource.class)
                            .addAsResource(new StringAsset("quarkus.http.body.delete-uploaded-files-on-end=true\n"),
                                    "application.properties");
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");

    @Test
    public void test() throws IOException {
        RestAssured.given()
                .multiPart("map",
                        "{\n" +
                                "  \"foo\": \"bar\",\n" +
                                "  \"sub\": {\n" +
                                "    \"foo2\": \"bar2\"\n" +
                                "  }\n" +
                                "}")
                .multiPart("person", "{\"first\": \"Bob\", \"last\": \"Builder\"}", "application/json")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .accept("application/json")
                .when()
                .post("/multipart/json")
                .then()
                .statusCode(200)
                .body("foo", equalTo("bar"))
                .body("sub.foo2", equalTo("bar2"))
                .body("person.first", equalTo("Bob"))
                .body("person.last", equalTo("Builder"))
                .body("htmlFileSize", equalTo(Files.readAllBytes(HTML_FILE.toPath()).length))
                .body("htmlFilePath", not(equalTo(HTML_FILE.toPath().toAbsolutePath().toString())))
                .body("htmlFileContentType", equalTo("text/html"));
    }
}
