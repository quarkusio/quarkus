package io.quarkus.resteasy.reactive.qute.deployment;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class TemplateResultTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class)
                    .addClass(Templates.class)
                    .addAsResource("templates/toplevel.txt")
                    .addAsResource("templates/HelloResource/hello.txt")
                    .addAsResource("templates/HelloResource/czech.txt")
                    .addAsResource("templates/HelloResource/typedTemplate.txt")
                    .addAsResource("templates/HelloResource/typedTemplate.html")
                    .addAsResource("templates/HelloResource/typedTemplatePrimitives.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/hello.txt"));

    @Test
    public void test() {
        when().get("/hello").then().statusCode(200).body(Matchers.is("Hello world!"));
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
        when().get("/hello/status-and-headers").then().statusCode(201).header("foo", "bar").body(Matchers.is("Hello world!"));
        when().get("/hello/rest-response").then().statusCode(202).body(Matchers.is("Hello world!"));
        when().get("/hello/response").then().statusCode(203).body(Matchers.is("Hello world!"));
    }

    @Test
    public void testNonAsciiOutput() {
        // The rendered template must be encoded correctly, both when returned directly
        // (rendered via chunks) and when wrapped in a Response
        byte[] expected = "Čau, Čolek! Toto je statický text.\n".getBytes(StandardCharsets.UTF_8);
        for (String path : new String[] { "hello/czech", "hello/czech-response" }) {
            Response response = when().get(path).then().statusCode(200)
                    .contentType(ContentType.TEXT).extract().response();
            assertThat(response.asByteArray()).isEqualTo(expected);
            assertThat(response.getHeader("Content-Length")).isEqualTo(String.valueOf(expected.length));
        }
    }

}
