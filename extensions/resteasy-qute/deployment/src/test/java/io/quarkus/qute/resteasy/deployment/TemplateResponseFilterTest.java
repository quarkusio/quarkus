package io.quarkus.qute.resteasy.deployment;

import static io.restassured.RestAssured.when;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TemplateResponseFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class)
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/hello.txt"));

    @Test
    public void testFilter() {
        when().get("/hello").then().body(Matchers.is("Hello world!"));
        when().get("/hello?name=Joe").then().body(Matchers.is("Hello Joe!"));
    }

}
