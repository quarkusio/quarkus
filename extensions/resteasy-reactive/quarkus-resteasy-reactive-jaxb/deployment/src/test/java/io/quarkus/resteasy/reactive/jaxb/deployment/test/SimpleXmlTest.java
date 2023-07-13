package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import javax.xml.namespace.QName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

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
            .withConfigurationResource("exclude-model-from-jaxb.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Person.class, ModelWithoutAnnotation.class, SimpleXmlResource.class,
                            io.quarkus.resteasy.reactive.jaxb.deployment.test.one.Model.class,
                            io.quarkus.resteasy.reactive.jaxb.deployment.test.two.Model.class,
                            ResourceWithModelSameName.class));

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

    @Test
    public void testResourceUsingModelWithSameName() {
        var one = new io.quarkus.resteasy.reactive.jaxb.deployment.test.one.Model();
        one.setName1("aa");

        var two = new io.quarkus.resteasy.reactive.jaxb.deployment.test.two.Model();
        two.setName2("bb");

        RestAssured.with().contentType(ContentType.XML).accept(ContentType.TEXT)
                .body(toXml(one))
                .post("/same-name/model-1")
                .then()
                .body(is("aa"));

        RestAssured.with().contentType(ContentType.XML).accept(ContentType.TEXT)
                .body(toXml(two))
                .post("/same-name/model-2")
                .then()
                .body(is("bb"));
    }

    @Test
    public void testSupportReturningJaxbElement() {
        Person response = RestAssured
                .get("/simple/person-as-jaxb-element")
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                .extract().as(Person.class);

        assertEquals("Bob", response.getFirst());
        assertEquals("Builder", response.getLast());
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
        @Produces(MediaType.APPLICATION_XML)
        @Path("/person")
        public Person getPerson() {
            Person person = new Person();
            person.setFirst("Bob");
            person.setLast("Builder");
            return person;
        }

        @GET
        @Produces(MediaType.APPLICATION_XML)
        @Path("/person-as-jaxb-element")
        public JAXBElement<Person> getPersonAsJaxbElement() {
            return new JAXBElement<>(new QName("person"), Person.class, getPerson());
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
            if (BlockingOperationControl.isBlockingAllowed()) {
                throw new RuntimeException("should have dispatched back to event loop");
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

    /**
     * Resource to reproduce https://github.com/quarkusio/quarkus/issues/31032.
     */
    @Path("/same-name")
    public static class ResourceWithModelSameName {
        @POST
        @Path("/model-1")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_XML)
        public String postModel1(io.quarkus.resteasy.reactive.jaxb.deployment.test.one.Model model) {
            return model.getName1();
        }

        @POST
        @Path("/model-2")
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_XML)
        public String postModel2(io.quarkus.resteasy.reactive.jaxb.deployment.test.two.Model model) {
            return model.getName2();
        }
    }
}
