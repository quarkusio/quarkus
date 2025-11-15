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
        // Because there is no entity, the fact that there is no datasource will not trigger an error:
        // the persistence unit will simply be automatically deactivated.
        // Panache will however raise an exception on first attempt to use a class that is not, in fact, an entity.
        RestAssured.when().get("/unannotatedEntity").then().statusCode(500).body(containsString("@Entity"))
                .body(not(containsString("NullPointer")));

        TEST.modifySourceFile(UnAnnotatedEntity.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//", "");
            }
        });

        // Once we do have entities, the persistence unit will be active,
        // but will fail to start since there is no datasource.
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

        RestAssured.when().get("/unannotatedEntity").then().statusCode(200);
    }

}
