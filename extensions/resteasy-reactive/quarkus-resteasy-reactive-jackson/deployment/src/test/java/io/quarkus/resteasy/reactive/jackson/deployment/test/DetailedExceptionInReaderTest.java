package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DetailedExceptionInReaderTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Person.class, TestEndpoint.class);
                }
            });

    @Test
    public void test() {
        RestAssured.with().accept("application/json").contentType("application/json")
                .body("{\"name\": \"foo\", \"age\": \"wrong\"}").put("/test")
                .then().statusCode(400).body(equalTo("{\"objectName\":\"Person\",\"attributeName\":\"age\","
                        + "\"line\":1,\"column\":24,\"value\":\"wrong\"}"));
    }

    @Path("test")
    public static class TestEndpoint {

        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @PUT
        public Person putJson(Person person) {
            return person;
        }
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
