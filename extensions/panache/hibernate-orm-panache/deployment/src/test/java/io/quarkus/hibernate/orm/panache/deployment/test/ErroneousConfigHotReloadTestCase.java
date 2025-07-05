package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ErroneousConfigHotReloadTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setAllowFailedStart(true)
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnAnnotatedEntity.class, UnAnnotatedEntityResource.class)
                    .addAsResource("application-commented-out.properties", "application.properties"));

    @Test
    public void test() {
        RestAssured.when()
                .get("/unannotatedEntity").then().statusCode(500)
                // Weirdly, in case of build errors, Quarkus will return the error as HTML, even if we set the content type to JSON...
                // Hence the &lt; / &gt;
                .body(containsString(
                        "Datasource '&lt;default&gt;' is not configured. To solve this, configure datasource '&lt;default&gt;'."))
                .body(not(containsString("NullPointer")));

        TEST.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("#", "");
            }
        });

        RestAssured.when().get("/unannotatedEntity").then().statusCode(500).body(containsString("@Entity"))
                .body(not(containsString("NullPointer")));

        TEST.modifySourceFile(UnAnnotatedEntity.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//", "");
            }
        });

        RestAssured.when().get("/unannotatedEntity").then().statusCode(200);
    }

}
