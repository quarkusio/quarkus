package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.multipart.other.OtherPackageFormDataBase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class MultipartInputWithAllUploadsTest extends AbstractMultipartTest {

    private static final Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setDeleteUploadedFilesOnEnd(false)
            .setUploadPath(uploadDir).setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(FormDataBase.class,
                            OtherPackageFormDataBase.class, FormDataWithAllUploads.class, Status.class,
                            MultipartResourceWithAllUploads.class);
                }
            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");
    private final File XML_FILE = new File("./src/test/resources/test.html");
    private final File TXT_FILE = new File("./src/test/resources/lorem.txt");

    @BeforeEach
    public void assertEmptyUploads() {
        Assertions.assertTrue(isDirectoryEmpty(uploadDir));
    }

    @AfterEach
    public void clearDirectory() {
        clearDirectory(uploadDir);
    }

    @Test
    public void testSimple() throws IOException {
        RestAssured.given().multiPart("name", "Alice").multiPart("active", "true").multiPart("num", "25")
                .multiPart("status", "WORKING").multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("xmlFile", XML_FILE, "text/xml").multiPart("txtFile", TXT_FILE, "text/plain")
                .accept("text/plain").when().post("/multipart-all/simple/2").then().statusCode(200)
                .body(equalTo("Alice - true - 50 - WORKING - 3 - text/plain"));

        // ensure that the 3 uploaded files where created on disk
        Assertions.assertEquals(3, uploadDir.toFile().listFiles().length);
    }
}
