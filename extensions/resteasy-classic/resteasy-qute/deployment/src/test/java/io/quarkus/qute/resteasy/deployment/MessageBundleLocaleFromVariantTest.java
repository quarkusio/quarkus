package io.quarkus.qute.resteasy.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleLocaleFromVariantTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AppMessages.class, AppMessageHelloResource.class)
                    .addAsResource(new StringAsset(
                            "{msg:hello_name('Georg')}"),
                            "templates/hello.html")
                    .addAsResource(new StringAsset(
                            "hello=Hallo Welt!\nhello_name=Hallo {name}!"),
                            "messages/msg_de.properties"));

    @Test
    public void testMessageBundles() {
        given().header("Accept-Language", "de-DE").when().get("/hello").then().body(is("Hallo Georg!"));

    }
}
