package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code quarkus.http.limits.max-multipart-file-size} bounds every file part of a multipart request on its own,
 * while text attributes stay bounded by {@code max-form-attribute-size}.
 */
public class MaxMultipartFileSizeTest {

    private static final int ONE_MEGABYTE = 1024 * 1024;

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class)
                            .addAsResource(new StringAsset("""
                                    quarkus.http.limits.max-body-size=30M
                                    quarkus.http.limits.max-form-attribute-size=2M
                                    quarkus.http.limits.max-multipart-file-size=1M
                                    quarkus.http.body.multipart.file-size-threshold=16K
                                    """),
                                    "application.properties");
                }
            });

    @Test
    public void fileLargerThanTheLimitIsRejected() {
        given()
                .multiPart("file", "large.bin", new byte[2 * ONE_MEGABYTE])
                .post("/test/file")
                .then()
                .statusCode(413);
    }

    @Test
    public void fileWithinTheLimitIsAccepted() {
        given()
                .multiPart("file", "small.bin", new byte[ONE_MEGABYTE / 2])
                .post("/test/file")
                .then()
                .statusCode(200)
                .body(Matchers.is(String.valueOf(ONE_MEGABYTE / 2)));
    }

    @Test
    public void limitAppliesToEachFileSeparately() {
        given()
                .multiPart("first", "first.bin", new byte[ONE_MEGABYTE * 3 / 4])
                .multiPart("second", "second.bin", new byte[ONE_MEGABYTE * 3 / 4])
                .post("/test/files")
                .then()
                .statusCode(200)
                .body(Matchers.is(String.valueOf(ONE_MEGABYTE * 3 / 2)));
    }

    @Test
    public void textAttributesAreNotBoundedByTheFileLimit() {
        given()
                .multiPart("text", new String(new char[ONE_MEGABYTE * 3 / 2]).replace('\0', 'a'))
                .post("/test/text")
                .then()
                .statusCode(200)
                .body(Matchers.is(String.valueOf(ONE_MEGABYTE * 3 / 2)));
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Path("/file")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String file(@RestForm("file") FileUpload file) {
            return String.valueOf(file.size());
        }

        @POST
        @Path("/files")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String files(@RestForm("first") FileUpload first, @RestForm("second") FileUpload second) {
            return String.valueOf(first.size() + second.size());
        }

        @POST
        @Path("/text")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String text(@RestForm("text") String text) {
            return String.valueOf(text.length());
        }
    }
}
