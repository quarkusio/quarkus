package io.quarkus.qute.deployment.currentrequest;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;

public class CurrentRequestTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyRoute.class)
                    .addAsResource(new StringAsset(
                            "Hello {inject:vertxRequest.getParam('name')}!"),
                            "templates/request.txt"));

    @Test
    public void testCurrentRequest() {
        when().get("/current-request?name=Joe").then().body(Matchers.is("Hello Joe!"));
    }

    public static class MyRoute {

        @Inject
        Template request;

        @Route
        String currentRequest() {
            return request.render();
        }
    }

}
