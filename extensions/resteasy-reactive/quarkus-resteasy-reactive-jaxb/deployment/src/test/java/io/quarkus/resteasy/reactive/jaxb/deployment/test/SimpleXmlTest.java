package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.common.annotation.NonBlocking;

public class SimpleXmlTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Person.class, ModelWithoutAnnotation.class, SimpleXmlResource.class));

    @Test
    public void testXml() {
        Person person = RestAssured.get("/simple/person")
                .then()
                .statusCode(200)
                .contentType("application/xml")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .extract().as(Person.class);

        assertEquals("Bob", person.getFirst());
        assertEquals("Builder", person.getLast());

        Person secondPerson = RestAssured
                .with()
                .body(toXml(person))
                .contentType("application/xml; charset=utf-8")
                .post("/simple/person")
                .then()
                .statusCode(200)
                .contentType("application/xml")
                .header("content-length", notNullValue())
                .header("transfer-encoding", nullValue())
                .extract().as(Person.class);

        assertEquals(person.getFirst(), secondPerson.getFirst());
        assertEquals(person.getLast(), secondPerson.getLast());
    }

    @Test
    public void testLargeXmlPost() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            sb.append("abc");
        }

        String longString = sb.toString();
        Person actual = RestAssured
                .with()
                .body(toXml(new Person(longString, longString)))
                .contentType("application/xml; charset=utf-8")
                .post("/simple/person-large")
                .then()
                .statusCode(200)
                .contentType("application/xml")
                .extract().as(Person.class);

        assertEquals(longString, actual.getFirst());
        assertEquals(longString, actual.getLast());
    }

    @Test
    public void testValidatedXml() {
        String person = toXml(new Person("Bob", "Builder"));
        RestAssured
                .with()
                .body(person)
                .contentType("application/xml")
                .post("/simple/person-validated")
                .then()
                .statusCode(200);

        RestAssured
                .with()
                .body(person)
                .contentType("application/xml")
                .post("/simple/person-invalid-result")
                .then()
                .statusCode(500);

        RestAssured
                .with()
                .body(toXml(new Person("Bob", null)))
                .accept("application/xml")
                .contentType("application/xml")
                .post("/simple/person-validated")
                .then()
                .statusCode(400)
                .contentType("application/xml");
    }

    @Test
    public void testAsyncXml() {
        RestAssured.get("/simple/async-person")
                .then()
                .body("person.first", Matchers.equalTo("Bob"))
                .body("person.last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testModelWithoutAnnotationAtRead() {
        ModelWithoutAnnotation actual = RestAssured.with().contentType(ContentType.XML)
                .get("/simple/model-without-annotation")
                .then()
                .extract().as(ModelWithoutAnnotation.class);

        assertEquals("My Message", actual.getMessage());
    }

    @Test
    public void testModelWithoutAnnotationAtWrite() {
        String expectedMessage = "My Message";
        ModelWithoutAnnotation request = new ModelWithoutAnnotation();
        request.setMessage(expectedMessage);

        ModelWithoutAnnotation response = RestAssured.with().contentType(ContentType.XML).accept(ContentType.XML)
                .body(toXml(request))
                .post("/simple/model-without-annotation")
                .then()
                .extract().as(ModelWithoutAnnotation.class);

        assertEquals(expectedMessage, response.getMessage());
    }

    private String toXml(Object person) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(person, sw);
        return sw.toString();
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Person {

        private String first;

        @NotBlank(message = "Title cannot be blank")
        private String last;

        public Person() {

        }

        public Person(String first, String last) {
            this.first = first;
            this.last = last;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }
    }

    public static class ModelWithoutAnnotation {

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Path("/simple")
    @NonBlocking
    public static class SimpleXmlResource {

        @GET
        @Path("/person")
        public Person getPerson() {
            Person person = new Person();
            person.setFirst("Bob");
            person.setLast("Builder");
            return person;
        }

        @POST
        @Path("/person")
        @Produces(MediaType.APPLICATION_XML)
        @Consumes(MediaType.APPLICATION_XML)
        public Person getPerson(Person person) {
            if (BlockingOperationControl.isBlockingAllowed()) {
                throw new RuntimeException("should not have dispatched");
            }
            return person;
        }

        @POST
        @Path("/person-large")
        @Produces(MediaType.APPLICATION_XML)
        @Consumes(MediaType.APPLICATION_XML)
        public Person personTest(Person person) {
            //large requests should get bumped from the IO thread
            if (!BlockingOperationControl.isBlockingAllowed()) {
                throw new RuntimeException("should have dispatched");
            }
            return person;
        }

        @POST
        @Path("/person-validated")
        @Produces(MediaType.APPLICATION_XML)
        @Consumes(MediaType.APPLICATION_XML)
        public Person getValidatedPerson(@Valid Person person) {
            return person;
        }

        @POST
        @Path("/person-invalid-result")
        @Produces(MediaType.APPLICATION_XML)
        @Consumes(MediaType.APPLICATION_XML)
        @Valid
        public Person getInvalidPersonResult(@Valid Person person) {
            person.setLast(null);
            return person;
        }

        @GET
        @Path("/async-person")
        @Produces(MediaType.APPLICATION_XML)
        public void getPerson(@Suspended AsyncResponse response) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Person person = new Person();
                    person.setFirst("Bob");
                    person.setLast("Builder");
                    response.resume(person);
                }
            }).start();
        }

        @GET
        @Path("/model-without-annotation")
        @Produces(MediaType.APPLICATION_XML)
        public ModelWithoutAnnotation getModelWithoutAnnotation() {
            ModelWithoutAnnotation model = new ModelWithoutAnnotation();
            model.setMessage("My Message");
            return model;
        }

        @POST
        @Path("/model-without-annotation")
        @Produces(MediaType.APPLICATION_XML)
        @Consumes(MediaType.APPLICATION_XML)
        public ModelWithoutAnnotation postModelWithoutAnnotation(ModelWithoutAnnotation request) {
            ModelWithoutAnnotation model = new ModelWithoutAnnotation();
            model.setMessage(request.getMessage());
            return model;
        }
    }
}
