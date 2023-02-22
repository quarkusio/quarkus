package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.multipart.other.OtherPackageFormDataBase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.vertx.core.http.HttpServerOptions;

public class TooLargeFormAttributeMultipartFormInputTest extends AbstractMultipartTest {

    private static final java.nio.file.Path uploadDir = Paths.get("file-uploads");

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setUploadPath(uploadDir)
            .setDeleteUploadedFilesOnEnd(false)
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, Status.class, FormDataBase.class, OtherPackageFormDataBase.class,
                                    FormData.class);
                }
            });

    private final File FORM_ATTR_SOURCE_FILE = new File("./src/test/resources/larger-than-default-form-attribute.txt");
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
    public void test() throws IOException {
        String fileContents = new String(Files.readAllBytes(FORM_ATTR_SOURCE_FILE.toPath()), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append(fileContents);
        }
        fileContents = sb.toString();
        Assertions.assertTrue(fileContents.length() > HttpServerOptions.DEFAULT_MAX_FORM_ATTRIBUTE_SIZE);
        given()
                .multiPart("active", "true")
                .multiPart("num", "25")
                .multiPart("status", "WORKING")
                .multiPart("htmlFile", HTML_FILE, "text/html")
                .multiPart("xmlFile", XML_FILE, "text/xml")
                .multiPart("txtFile", TXT_FILE, "text/plain")
                .multiPart("name", fileContents)
                .accept("text/plain")
                .when()
                .post("/test")
                .then()
                .statusCode(413);

        // ensure that no files where created on disk
        // as RESTEasy Reactive doesn't wait for the files to be deleted before returning the HTTP response,
        // we need to wait in the test
        awaitUploadDirectoryToEmpty(uploadDir);
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        @SuppressWarnings({ "removal" })
        public String hello(@MultipartForm FormData data) {
            return data.getName();
        }
    }

}
