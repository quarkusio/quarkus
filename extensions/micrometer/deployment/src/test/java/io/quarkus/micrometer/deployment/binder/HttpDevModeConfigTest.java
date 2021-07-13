package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.test.HelloResource;
import io.quarkus.test.QuarkusDevModeTest;

public class HttpDevModeConfigTest {
    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class)
                    .add(new StringAsset("quarkus.micrometer.binder-enabled-default=false\n" +
                            "quarkus.micrometer.binder.http-client.enabled=true\n" +
                            "quarkus.micrometer.binder.http-server.enabled=true\n" +
                            "quarkus.micrometer.binder.http-server.ignore-patterns=/http\n" +
                            "quarkus.micrometer.binder.vertx.enabled=true\n"), "application.properties"));

    @Test
    public void test() throws Exception {

        when().get("/hello/one").then().statusCode(200);
        when().get("/hello/two").then().statusCode(200);
        when().get("/hello/three").then().statusCode(200);
        when().get("/q/metrics").then().statusCode(200)
                .body(Matchers.containsString("/hello/{message}"));

        test.modifyResourceFile("application.properties",
                s -> s.replace("quarkus.micrometer.binder.http-server.ignore-patterns=/http",
                        "quarkus.micrometer.binder.http-server.match-patterns=/hello/.*=/goodbye/{message}"));

        when().get("/hello/one").then().statusCode(200);
        when().get("/hello/two").then().statusCode(200);
        when().get("/hello/three").then().statusCode(200);
        when().get("/q/metrics").then().statusCode(200)
                .body(Matchers.containsString("/goodbye/{message}"));
    }

}
