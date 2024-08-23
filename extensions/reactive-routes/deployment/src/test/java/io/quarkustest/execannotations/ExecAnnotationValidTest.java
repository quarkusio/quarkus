package io.quarkustest.execannotations;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;

public class ExecAnnotationValidTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MyService.class));

    @Test
    public void test() {
        when().get("/").then().statusCode(200).body(is("Hello world!"));
    }

    static class MyService {
        @Route(path = "/")
        @Blocking
        String hello() {
            return "Hello world!";
        }
    }
}
