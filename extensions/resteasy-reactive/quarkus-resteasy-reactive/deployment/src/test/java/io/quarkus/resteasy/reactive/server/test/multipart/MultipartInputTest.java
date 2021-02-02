package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.multipart.other.OtherPackageFormDataBase;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultipartInputTest {

    private static final File uploadDir = new File("./file-uploads"); // the default used by Quarkus

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FormDataBase.class, OtherPackageFormDataBase.class, FormData.class, Status.class,
                                    OtherFormData.class,
                                    OtherFormDataBase.class,
                                    MultipartResource.class, OtherMultipartResource.class);
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File XML_FILE = new File("./src/test/resources/test.html");
    private final File TXT_FILE = new File("./src/test/resources/lorem.txt");

    @BeforeEach
    public void assertEmptyUploads() {
        Assertions.assertEquals(0, uploadDir.listFiles().length);
    }

    @AfterEach
    public void clearDirectory() {
        for (File file : uploadDir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    @Test
    public void testSimple() {
        RestAssured.given()
                .multiPart("name", "Alice")
                .multiPart("active", "true")
                .multiPart("num", "25")
                .multiPart("status", "WORKING")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("xmlFile", XML_FILE, "text/xml")
                .multiPart("txtFile", TXT_FILE, "text/plain")
                .accept("text/plain")
                .when()
                .post("/multipart/simple/2")
                .then()
                .statusCode(200)
                .body(equalTo("Alice - true - 50 - WORKING - text/html - true - true"));

        // ensure that the 3 uploaded files where created on disk
        Assertions.assertEquals(3, uploadDir.listFiles().length);
    }

    @Test
    public void testBlocking() throws IOException {
        RestAssured.given()
                .multiPart("name", "Trudy")
                .multiPart("num", "20")
                .multiPart("status", "SLEEPING")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("xmlFile", XML_FILE, "text/xml")
                .multiPart("txtFile", TXT_FILE, "text/plain")
                .accept("text/plain")
                .when()
                .post("/multipart/blocking?times=2")
                .then()
                .statusCode(200)
                .body(equalTo("Trudy - 40 - SLEEPING"))
                .header("html-size", equalTo(fileSizeAsStr(HTML_FILE)))
                // test that file was actually upload and that the web application isn't sharing the file with the test...
                .header("html-path", not(equalTo(filePath(HTML_FILE))))
                .header("xml-size", equalTo(fileSizeAsStr(XML_FILE)))
                .header("xml-path", not(equalTo(filePath(XML_FILE))))
                .header("txt-size", equalTo(fileSizeAsStr(TXT_FILE)))
                .header("txt-path", not(equalTo(filePath(TXT_FILE))));

        // ensure that the 3 uploaded files where created on disk
        Assertions.assertEquals(3, uploadDir.listFiles().length);
    }

    @Test
    public void testOther() {
        RestAssured.given()
                .multiPart("first", "foo")
                .multiPart("last", "bar")
                .accept("text/plain")
                .when()
                .post("/otherMultipart/simple")
                .then()
                .statusCode(200)
                .body(equalTo("foo - bar - final - static"));

        Assertions.assertEquals(0, uploadDir.listFiles().length);
    }

    private String filePath(File file) {
        return file.toPath().toAbsolutePath().toString();
    }

    private String fileSizeAsStr(File file) throws IOException {
        return "" + Files.readAllBytes(file.toPath()).length;
    }

}
