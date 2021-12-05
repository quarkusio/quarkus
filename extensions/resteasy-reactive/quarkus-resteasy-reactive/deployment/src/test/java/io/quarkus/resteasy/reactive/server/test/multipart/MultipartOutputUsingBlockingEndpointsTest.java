package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import javax.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.multipart.other.OtherPackageFormDataBase;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class MultipartOutputUsingBlockingEndpointsTest extends AbstractMultipartTest {

    private static final String EXPECTED_CONTENT_DISPOSITION_PART = "Content-Disposition: form-data; name=\"%s\"";
    private static final String EXPECTED_CONTENT_DISPOSITION_FILE_PART = "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"";
    private static final String EXPECTED_CONTENT_TYPE_PART = "Content-Type: %s";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MultipartOutputResource.class, MultipartOutputResponse.class,
                            MultipartOutputFileResponse.class,
                            Status.class, FormDataBase.class, OtherPackageFormDataBase.class));

    @Test
    public void testSimple() {
        String response = RestAssured.get("/multipart/output/simple")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertContainsValue(response, "name", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_NAME);
        assertContainsValue(response, "custom-surname", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_SURNAME);
        assertContainsValue(response, "custom-status", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_STATUS);
        assertContainsValue(response, "active", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_ACTIVE);
        assertContainsValue(response, "values", MediaType.TEXT_PLAIN, "[one, two]");
        assertContainsValue(response, "num", MediaType.TEXT_PLAIN, "0");
    }

    @Test
    public void testString() {
        RestAssured.get("/multipart/output/string")
                .then()
                .statusCode(200)
                .body(equalTo(MultipartOutputResource.RESPONSE_NAME));
    }

    @Test
    public void testWithFiles() {
        String response = RestAssured
                .given()
                .get("/multipart/output/with-file")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .log().all()
                .extract().asString();

        assertContainsValue(response, "name", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_NAME);
        assertContainsFile(response, "file", MediaType.APPLICATION_OCTET_STREAM, "lorem.txt");
    }

    private void assertContainsFile(String response, String name, String contentType, String fileName) {
        String[] lines = response.split("--");
        assertThat(lines).anyMatch(line -> line.contains(String.format(EXPECTED_CONTENT_DISPOSITION_FILE_PART, name, fileName))
                && line.contains(String.format(EXPECTED_CONTENT_TYPE_PART, contentType)));
    }

    private void assertContainsValue(String response, String name, String contentType, Object value) {
        String[] lines = response.split("--");
        assertThat(lines).anyMatch(line -> line.contains(String.format(EXPECTED_CONTENT_DISPOSITION_PART, name))
                && line.contains(String.format(EXPECTED_CONTENT_TYPE_PART, contentType))
                && line.contains(value.toString()));
    }
}
