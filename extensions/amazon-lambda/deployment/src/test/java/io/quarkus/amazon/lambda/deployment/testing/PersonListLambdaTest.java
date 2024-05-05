package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PersonListLambdaTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(PersonListLambda.class, Person.class));

    @Test
    void testFruitsLambda() throws Exception {

        List<Person> personList = new ArrayList<>();
        personList.add(new Person("Chris"));
        personList.add(new Person("Fred"));

        given()
                .body(personList)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Chris Fred"));
    }
}
