package io.quarkus.qute.resteasy.deployment;

import static io.restassured.RestAssured.when;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class TemplateResponseFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class)
                    .addClass(Templates.class)
                    .addAsResource("templates/toplevel.txt")
                    .addAsResource("templates/HelloResource/hello.txt")
                    .addAsResource("templates/HelloResource/typedTemplate.txt")
                    .addAsResource("templates/HelloResource/typedTemplate.html")
                    .addAsResource("templates/HelloResource/typedTemplatePrimitives.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/hello.txt"));

    @Test
    public void testFilter() {
        when().get("/hello").then().body(Matchers.is("Hello world!"));
        when().get("/hello?name=Joe").then().body(Matchers.is("Hello Joe!"));
        when().get("/hello/no-injection").then().body(Matchers.is("Salut world!"));
        when().get("/hello/no-injection?name=Joe").then().body(Matchers.is("Salut Joe!"));
        RestAssured.given().accept(ContentType.TEXT).get("/hello/native/typed-template").then()
                .body(Matchers.is("Salut world!"));
        RestAssured.given().accept(ContentType.TEXT).get("/hello/native/typed-template?name=Joe").then()
                .body(Matchers.is("Salut Joe!"));
        RestAssured.given().accept(ContentType.HTML).get("/hello/native/typed-template?name=Joe").then()
                .body(Matchers.is("<html>Salut Joe!</html>"));
        when().get("/hello/native/typed-template-primitives").then()
                .body(Matchers.is("Byte: 0 Short: 1 Int: 2 Long: 3 Char: a Boolean: true Float: 4.0 Double: 5.0"));
        when().get("/hello/native/toplevel?name=Joe").then().body(Matchers.is("Salut Joe!"));
    }

}
