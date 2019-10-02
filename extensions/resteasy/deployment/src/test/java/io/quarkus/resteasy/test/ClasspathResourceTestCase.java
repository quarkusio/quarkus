package io.quarkus.resteasy.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ClasspathResourceTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RootResource.class)
                    .addAsManifestResource(new StringAsset("hello"), "resources/other/hello.txt")
                    .addAsManifestResource(new StringAsset("index"), "resources/index.html")
                    .addAsManifestResource(new StringAsset("stuff"), "resources/stuff.html"));

    @Test
    public void testRootResource() {
        RestAssured.when().get("/other/hello.txt").then().body(Matchers.is("hello"));
        RestAssured.when().get("/stuff.html").then().body(Matchers.is("stuff"));
        RestAssured.when().get("/index.html").then().body(Matchers.is("index"));
        RestAssured.when().get("/").then().body(Matchers.is("index"));
    }

}
