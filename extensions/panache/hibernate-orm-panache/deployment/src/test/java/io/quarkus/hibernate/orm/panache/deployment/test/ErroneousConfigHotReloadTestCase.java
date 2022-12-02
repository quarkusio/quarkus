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
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnAnnotatedEntity.class, UnAnnotatedEntityResource.class)
                    .addAsResource("application-commented-out.properties", "application.properties"));

    @Test
    public void test() {
        RestAssured.when().get("/unannotatedEntity").then().statusCode(500)
                .body(containsString("The default datasource has not been properly configured."))
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
