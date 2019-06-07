package io.quarkus.camel.component.servlet.test;

import org.apache.camel.builder.RouteBuilder;
import org.hamcrest.core.IsEqual;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MinimalConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Routes.class)
                    .addAsResource(new StringAsset("quarkus.camel.servlet.url-patterns=/*\n"),
                            "application.properties"));

    @Test
    public void minimal() {
        RestAssured.when().get("/rest-get").then().body(IsEqual.equalTo("GET: /rest-get"));
        RestAssured.when().post("/rest-post").then().body(IsEqual.equalTo("POST: /rest-post"));
        RestAssured.when().get("/hello").then().body(IsEqual.equalTo("GET: /hello"));
    }

    public static class Routes extends RouteBuilder {

        @Override
        public void configure() {

            rest()
                    .get("/rest-get")
                    .route()
                    .setBody(constant("GET: /rest-get"))
                    .endRest()
                    .post("/rest-post")
                    .route()
                    .setBody(constant("POST: /rest-post"))
                    .endRest();

            from("servlet://hello?matchOnUriPrefix=true")
                    .setBody(constant("GET: /hello"));
        }
    }

}
