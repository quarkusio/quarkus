package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.response.Response;

/**
 * With {@code quarkus.http.body.multipart.file-size-threshold} set, file parts up to that size are kept in memory
 * while larger ones are written to the uploads directory.
 * See <a href="https://github.com/quarkusio/quarkus/issues/43127">GitHub issue #43127</a>.
 */
public class MultipartFileSizeThresholdTest {

    private static final int THRESHOLD = 10 * 1024;
    private static final byte[] SMALL = bytes(THRESHOLD);
    private static final byte[] LARGE = bytes(THRESHOLD + 1);

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Form.class))
            .overrideRuntimeConfigKey("quarkus.http.body.multipart.file-size-threshold", "10K")
            .overrideRuntimeConfigKey("quarkus.http.body.uploads-directory", "target/multipart-threshold-uploads")
            .overrideRuntimeConfigKey("quarkus.http.body.delete-uploaded-files-on-end", "false");

    @Test
    void smallPartsStayInMemoryAndLargePartsGoToDisk() {
        Response response = given()
                .multiPart("small", "small.bin", SMALL, MediaType.APPLICATION_OCTET_STREAM)
                .multiPart("large", "large.bin", LARGE, MediaType.APPLICATION_OCTET_STREAM)
                .multiPart("text", "hello")
                .post("/multipart/input");
        response.then().statusCode(200);
        assertThat(response.asString().lines())
                .containsExactlyInAnyOrder(
                        "small:inMemory=true,size=" + SMALL.length,
                        "large:inMemory=false,size=" + LARGE.length,
                        "text:hello");
    }

    @Test
    void inMemoryPartIsWrittenToTheUploadsDirectoryWhenAFileIsRequested() throws IOException {
        Response response = given()
                .multiPart("small", "small.bin", SMALL, MediaType.APPLICATION_OCTET_STREAM)
                .post("/multipart/uploads");
        response.then().statusCode(200);
        java.nio.file.Path path = Paths.get(response.asString());
        assertThat(path).exists();
        assertThat(path.toAbsolutePath().getParent())
                .isEqualTo(Paths.get("target/multipart-threshold-uploads").toAbsolutePath());
        assertThat(Files.readAllBytes(path)).isEqualTo(SMALL);
    }

    @Test
    void inMemoryPartsAreAvailableThroughTheTypedParameters() {
        given()
                .multiPart("small", "small.bin", SMALL, MediaType.APPLICATION_OCTET_STREAM)
                .multiPart("text", "text.txt", "hello".getBytes(), MediaType.TEXT_PLAIN)
                .post("/multipart/typed")
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.equalTo("bytes=" + SMALL.length + ",stream=" + SMALL.length + ",text=hello"));
    }

    private static byte[] bytes(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    @Path("/multipart")
    public static class Resource {

        @POST
        @Path("input")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String input(MultipartFormDataInput input) throws IOException {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, java.util.Collection<FormValue>> entry : input.getValues().entrySet()) {
                for (FormValue value : entry.getValue()) {
                    sb.append(entry.getKey()).append(':');
                    if (value.isFileItem()) {
                        FileItem item = value.getFileItem();
                        sb.append("inMemory=").append(item.isInMemory()).append(",size=").append(item.getFileSize());
                    } else {
                        sb.append(value.getValue());
                    }
                    sb.append('\n');
                }
            }
            return sb.toString();
        }

        @POST
        @Path("uploads")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploads(@RestForm(FileUpload.ALL) List<FileUpload> uploads) {
            return uploads.get(0).uploadedFile().toString();
        }

        @POST
        @Path("typed")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String typed(Form form) throws IOException {
            try (InputStream is = form.smallStream) {
                return "bytes=" + form.small.length + ",stream=" + is.readAllBytes().length + ",text=" + form.text;
            }
        }
    }

    public static class Form {

        @RestForm("small")
        public byte[] small;

        @RestForm("small")
        public InputStream smallStream;

        @RestForm("text")
        public String text;
    }
}
