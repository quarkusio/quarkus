package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
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

public class MultipartOptionalInputTest extends AbstractMultipartTest {

    private static final Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setDeleteUploadedFilesOnEnd(false)
            .setUploadPath(uploadDir).setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(FormDataBase.class,
                            OtherPackageFormDataBase.class, FormData.class, Status.class, OtherFormData.class,
                            FormDataSameFileName.class, OtherFormDataBase.class, MultipartResource.class,
                            OtherMultipartResource.class);
                }

            });

    private final File HTML_FILE = new File("./src/test/resources/test.html");

    @BeforeEach
    public void assertEmptyUploads() {
        Assertions.assertTrue(isDirectoryEmpty(uploadDir));
    }

    @AfterEach
    public void clearDirectory() {
        clearDirectory(uploadDir);
    }

    @Test
    public void testUploadWithSomeFilesMissing() {
        RestAssured.given().multiPart("name", "Alice").multiPart("active", "true").multiPart("num", "25")
                .multiPart("status", "WORKING").multiPart("htmlFile", HTML_FILE, "text/html").accept("text/plain")
                .when().post("/multipart/optional").then().statusCode(200)
                .body(equalTo("Alice - true - 25 - WORKING - true - false - false"));

        // ensure that the 1 uploaded file was created on disk
        File[] uploadedFiles = uploadDir.toFile().listFiles();
        Assertions.assertNotNull(uploadedFiles);
        Assertions.assertEquals(1, uploadedFiles.length);
    }
}
