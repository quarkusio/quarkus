package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.core.MediaType;

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

public class MultipartFileContentTypeTest extends AbstractMultipartTest {

    private static final Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setDeleteUploadedFilesOnEnd(false)
            .setUploadPath(uploadDir)
            .setFileContentTypes(List.of(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_SVG_XML))
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(FormDataBase.class,
                            OtherPackageFormDataBase.class, FormData.class, Status.class, OtherFormData.class,
                            FormDataSameFileName.class, OtherFormDataBase.class, MultipartResource.class,
                            OtherMultipartResource.class);
                }

            });

    private final File FILE = new File("./src/test/resources/test.html");

    @BeforeEach
    public void assertEmptyUploads() {
        Assertions.assertTrue(isDirectoryEmpty(uploadDir));
    }

    @AfterEach
    public void clearDirectory() {
        clearDirectory(uploadDir);
    }

    @Test
    public void testFilePartWithExpectedContentType() throws IOException {
        RestAssured.given()
                .multiPart("octetStream", null, Files.readAllBytes(FILE.toPath()), MediaType.APPLICATION_OCTET_STREAM)
                .multiPart("svgXml", null, Files.readAllBytes(FILE.toPath()), MediaType.APPLICATION_SVG_XML)
                .accept("text/plain").when().post("/multipart/optional").then().statusCode(200);

        // ensure that the 2 uploaded files where created on disk
        Assertions.assertEquals(2, uploadDir.toFile().listFiles().length);
    }
}
