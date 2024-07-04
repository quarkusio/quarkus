package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.amazon.lambda.deployment.testing.model.OutputPerson;
import io.quarkus.test.QuarkusUnitTest;

public class AbstractInputCollectionOutputCollectionLambdaImplTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(AbstractInputCollectionOutputCollectionLambdaImpl.class, AbstractInputCollectionOutputCollection.class,
                    InputPerson.class, OutputPerson.class));

    @Test
    void abstractRequestHandler_InputCollectionInputPerson_OutputCollectionOutputPerson() {

        List<InputPerson> personList = new ArrayList<>();
        personList.add(new InputPerson("Chris"));
        personList.add(new InputPerson("Fred"));

        given()
                .body(personList)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body("", hasItem(hasEntry("outputname", "Chris"))) // OutputPerson serializes name with key outputname
                .body("", hasItem(hasEntry("outputname", "Fred")))
                .body("", not(hasItem(hasEntry("name", "Chris")))) // make sure that there is no key name
                .body("", not(hasItem(hasEntry("name", "Fred"))));
    }
}
