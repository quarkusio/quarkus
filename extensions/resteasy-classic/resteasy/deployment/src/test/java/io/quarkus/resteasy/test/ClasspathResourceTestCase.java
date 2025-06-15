package io.quarkus.resteasy.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ClasspathResourceTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(RootResource.class).addAsManifestResource(new StringAsset("hello"), "resources/other/hello.txt")
            .addAsManifestResource(new StringAsset("index"), "resources/index.html")
            .addAsManifestResource(new StringAsset("stuff"), "resources/stuff.html"));

    @Test
    public void testRootResource() {

        RestAssured.get("/other/hello.txt").then().statusCode(200).body(is("hello"));

        RestAssured.get("/stuff.html").then().statusCode(200).body(is("stuff"));

        RestAssured.get("/index.html").then().statusCode(200).body(is("index"));

        RestAssured.get("/").then().statusCode(200).body(is("index"));
    }

}
