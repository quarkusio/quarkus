package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class NestedMessageBundleDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest testConfig = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyNestedMessages.class, TestRouteConfig.class));

    @Test
    public void testMessages() {
        assertEquals("Hello Jachym!", RestAssured.get("test").then().statusCode(200).extract().body().asString());
        testConfig.modifySourceFile(NestedMessageBundleDevModeTest.class, s -> s.replace("Hello {name}!", "Hi {name}!"));
        assertEquals("Hi Jachym!", RestAssured.get("test").then().statusCode(200).extract().body().asString());
    }

    @MessageBundle
    public interface MyNestedMessages {

        @Message("Hello {name}!")
        String hello_name(String name);

    }

    public static class TestRouteConfig {

        void addTestRoute(@Observes Router router) {
            router.route("/test")
                    .produces("text/plain")
                    .handler(rc -> rc.response().end(MessageBundles.get(MyNestedMessages.class).hello_name("Jachym")));
        }

    }

}
