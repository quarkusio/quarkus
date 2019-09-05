package io.quarkus.resteasy.test;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class RestEasyDevModeTestCase {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(PostResource.class);
                }
            });

    @Test
    public void testRESTeasyHotReplacement() {
        RestAssured.given().body("Stuart")
                .when()
                .post("/post")
                .then()
                .content(Matchers.equalTo("Hello: Stuart"));
        test.modifySourceFile(PostResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("Hello:", "Hi:");
            }
        });
        RestAssured.given().body("Stuart")
                .when()
                .post("/post")
                .then()
                .content(Matchers.equalTo("Hi: Stuart"));
    }
}
