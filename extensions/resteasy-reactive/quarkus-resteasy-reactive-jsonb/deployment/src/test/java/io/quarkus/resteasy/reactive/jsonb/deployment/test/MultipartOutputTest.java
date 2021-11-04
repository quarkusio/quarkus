package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class MultipartOutputTest {
    private static final String EXPECTED_CONTENT_DISPOSITION_PART = "Content-Disposition: form-data; name=\"%s\"";
    private static final String EXPECTED_CONTENT_TYPE_PART = "Content-Type: %s";
    private static final String EXPECTED_RESPONSE_NAME = "a name";
    private static final String EXPECTED_RESPONSE_PERSON_NAME = "Michal";
    private static final int EXPECTED_RESPONSE_PERSON_AGE = 23;
    private static final String EXPECTED_RESPONSE_PERSON = "{\"age\":" + EXPECTED_RESPONSE_PERSON_AGE
            + ",\"name\":\"" + EXPECTED_RESPONSE_PERSON_NAME + "\"}";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MultipartOutputResource.class, MultipartOutputResponse.class, Person.class));

    @Test
    public void testSimple() {
        String response = RestAssured.get("/multipart/output")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .log().all()
                .extract().asString();

        assertContains(response, "name", MediaType.TEXT_PLAIN, EXPECTED_RESPONSE_NAME);
        assertContains(response, "person", MediaType.APPLICATION_JSON, EXPECTED_RESPONSE_PERSON);
    }

    private void assertContains(String response, String name, String contentType, Object value) {
        String[] lines = response.split("--");
        assertThat(lines).anyMatch(line -> line.contains(String.format(EXPECTED_CONTENT_DISPOSITION_PART, name))
                && line.contains(String.format(EXPECTED_CONTENT_TYPE_PART, contentType))
                && line.contains(value.toString()));
    }

    @Path("/multipart/output")
    private static class MultipartOutputResource {

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartOutputResponse simple() {
            MultipartOutputResponse response = new MultipartOutputResponse();
            response.name = EXPECTED_RESPONSE_NAME;
            response.person = new Person();
            response.person.name = EXPECTED_RESPONSE_PERSON_NAME;
            response.person.age = EXPECTED_RESPONSE_PERSON_AGE;
            return response;
        }

    }

    private static class MultipartOutputResponse {

        @RestForm
        String name;

        @RestForm
        @PartType(MediaType.APPLICATION_JSON)
        Person person;
    }

    public static class Person {
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
}
