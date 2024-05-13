package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class MultipartTest {
    private static final String EXPECTED_CONTENT_DISPOSITION_PART = "Content-Disposition: form-data; name=\"%s\"";
    private static final String EXPECTED_CONTENT_TYPE_PART = "Content-Type: %s";
    private static final String EXPECTED_RESPONSE_NAME = "a name";
    private static final String EXPECTED_RESPONSE_PERSON_NAME = "Michal";
    private static final int EXPECTED_RESPONSE_PERSON_AGE = 23;
    private static final String EXPECTED_RESPONSE_PERSON = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<person>"
            + "<age>" + EXPECTED_RESPONSE_PERSON_AGE + "</age>"
            + "<name>" + EXPECTED_RESPONSE_PERSON_NAME + "</name>"
            + "</person>";
    private static final String SCHOOL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<school>"
            + "<name>Divino Pastor</name>"
            + "</school>";
    private final File HTML_FILE = new File("./src/test/resources/test.html");

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MultipartOutputResource.class, MultipartOutputResponse.class, Person.class));

    @Test
    public void testOutput() {
        String response = RestAssured.get("/multipart/output")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertContains(response, "name", MediaType.TEXT_PLAIN, EXPECTED_RESPONSE_NAME);
        assertContains(response, "person", MediaType.TEXT_XML, EXPECTED_RESPONSE_PERSON);
    }

    @Test
    public void testInput() {
        String response = RestAssured
                .given()
                .multiPart("name", "John")
                .multiPart("school", SCHOOL, MediaType.APPLICATION_XML)
                .post("/multipart/input")
                .then()
                .statusCode(200)
                .extract().asString();

        assertThat(response).isEqualTo("John-Divino Pastor");
    }

    @Test
    public void testInputParam() {
        String response = RestAssured
                .given()
                .multiPart("name", "John")
                .multiPart("school", SCHOOL, MediaType.APPLICATION_XML)
                .post("/multipart/param/input")
                .then()
                .statusCode(200)
                .extract().asString();

        assertThat(response).isEqualTo("John-Divino Pastor");
    }

    @Test
    public void testInputFile() throws IOException {
        String response = RestAssured
                .given()
                .multiPart("file", HTML_FILE, "text/html")
                .post("/multipart/input/file")
                .then()
                .statusCode(200)
                .extract().asString();

        assertThat(response).isEqualTo(String.valueOf(Files.readAllBytes(HTML_FILE.toPath()).length));
    }

    private void assertContains(String response, String name, String contentType, Object value) {
        String[] lines = response.split("--");
        assertThat(lines).anyMatch(line -> line.contains(String.format(EXPECTED_CONTENT_DISPOSITION_PART, name))
                && line.contains(String.format(EXPECTED_CONTENT_TYPE_PART, contentType))
                && line.contains(value.toString()));
    }

    @Path("/multipart")
    private static class MultipartOutputResource {

        @GET
        @Path("/output")
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartOutputResponse output() {
            MultipartOutputResponse response = new MultipartOutputResponse();
            response.name = EXPECTED_RESPONSE_NAME;
            response.person = new Person();
            response.person.name = EXPECTED_RESPONSE_PERSON_NAME;
            response.person.age = EXPECTED_RESPONSE_PERSON_AGE;
            return response;
        }

        @POST
        @Path("/input")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String input(@BeanParam MultipartInput input) {
            return input.name + "-" + input.school.name;
        }

        @POST
        @Path("/param/input")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String input(@RestForm String name,
                @RestForm @PartType(MediaType.APPLICATION_XML) School school) {
            return name + "-" + school.name;
        }

        @POST
        @Path("/input/file")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public int inputFile(@BeanParam FileUploadData data) throws IOException {
            return Files.readAllBytes(data.fileUpload.filePath()).length;
        }

    }

    private static class MultipartOutputResponse {

        @RestForm
        String name;

        @RestForm
        @PartType(MediaType.TEXT_XML)
        Person person;
    }

    public static class FileUploadData {
        @FormParam("file")
        FileUpload fileUpload;
    }

    public static class MultipartInput {

        @RestForm
        String name;

        @RestForm
        @PartType(MediaType.APPLICATION_XML)
        School school;
    }

    private static class Person {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    private static class School {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
