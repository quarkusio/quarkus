package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
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
                            MultipartOutputFileResponse.class, MultipartOutputMultipleFileResponse.class,
                            MultipartOutputMultipleFileDownloadResponse.class, MultipartOutputSingleFileDownloadResponse.class,
                            Status.class, FormDataBase.class, OtherPackageFormDataBase.class, PathFileDownload.class));

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
    public void testRestResponse() {
        String response = RestAssured.get("/multipart/output/rest-response")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .header("foo", "bar")
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
    public void testWithFile() {
        String response = RestAssured
                .given()
                .get("/multipart/output/with-file")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertContainsValue(response, "name", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_NAME);
        assertContainsFile(response, "file", MediaType.APPLICATION_OCTET_STREAM, "lorem.txt");
    }

    @Test
    public void testWithSingleFileDownload() {
        String response = RestAssured
                .given()
                .get("/multipart/output/with-single-file-download")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertContainsFile(response, "one", MediaType.APPLICATION_OCTET_STREAM, "test.xml");
    }

    @Test
    public void testWithMultipleFiles() {
        String response = RestAssured
                .given()
                .get("/multipart/output/with-multiple-file")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertContainsValue(response, "name", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_NAME);
        assertContainsFile(response, "files", MediaType.APPLICATION_OCTET_STREAM, "lorem.txt");
        assertContainsFile(response, "files", MediaType.APPLICATION_OCTET_STREAM, "test.xml");
    }

    @Test
    public void testWithMultipleFileDownload() {
        String response = RestAssured
                .given()
                .get("/multipart/output/with-multiple-file-download")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertContainsValue(response, "name", MediaType.TEXT_PLAIN, MultipartOutputResource.RESPONSE_NAME);
        assertContainsFile(response, "files", MediaType.APPLICATION_OCTET_STREAM, "lorem.txt");
        assertContainsFile(response, "files", MediaType.APPLICATION_OCTET_STREAM, "test.xml");
    }

    @EnabledIfSystemProperty(named = "test-resteasy-reactive-large-files", matches = "true")
    @Test
    public void testWithLargeFiles() {
        RestAssured.given()
                .get("/multipart/output/with-large-file")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200);
    }

    @Test
    public void testWithNullFields() {
        RestAssured
                .given()
                .get("/multipart/output/with-null-fields")
                .then()
                .contentType(ContentType.MULTIPART)
                .log().all()
                .statusCode(200); // should return 200 with no parts
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
