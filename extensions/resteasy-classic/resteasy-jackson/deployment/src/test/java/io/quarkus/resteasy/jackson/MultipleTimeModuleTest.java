package io.quarkus.resteasy.jackson;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

// this test really belongs in the jackson module, but it's been added here to avoid test classpath issues
public class MultipleTimeModuleTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TimeCustomizer.class, DateDto.class, HelloResource.class));

    @Test
    public void testDateIsAlwaysInTheExpectedFormat() {
        verifyExpectedResult();

        modifyResource();
        verifyExpectedResult();

        modifyResource();
        verifyExpectedResult();

        modifyResource();
        verifyExpectedResult();
    }

    private void verifyExpectedResult() {
        RestAssured.get("/hello").then()
                .statusCode(200)
                .body(containsString("Z"), not(containsString("+")));
    }

    private void modifyResource() {
        TEST.modifySourceFile(TimeCustomizer.class, s -> s.replace("hello",
                "hello2"));
    }

}
