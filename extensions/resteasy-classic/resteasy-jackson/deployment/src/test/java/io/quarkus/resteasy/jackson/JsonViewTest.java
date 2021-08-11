package io.quarkus.resteasy.jackson;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JsonViewTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(JsonViewResource.class));

    @Test
    public void testView1() {
        RestAssured.get("/json-view/view1").then()
                .statusCode(200)
                .body("property1", equalTo("value1"))
                .and()
                .body("property2", is(nullValue()));
    }

    @Test
    public void testView2() {
        RestAssured.get("/json-view/view2").then()
                .statusCode(200)
                .body("property1", is(nullValue()))
                .and()
                .body("property2", equalTo("value2"));
    }
}
