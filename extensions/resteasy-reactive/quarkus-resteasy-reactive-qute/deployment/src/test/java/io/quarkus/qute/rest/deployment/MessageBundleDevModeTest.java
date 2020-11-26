package io.quarkus.qute.rest.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class MessageBundleDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AppMessages.class, AppMessageHelloResource.class)
                    .addAsResource(new StringAsset(
                            "{msg:hello_name('Georg')}"),
                            "templates/hello.html")
                    .addAsResource(new StringAsset(
                            "hello=Hallo Welt!\nhello_name=Hallo {name}!"),
                            "messages/msg_de.properties"));

    @Test
    public void testMessageBundles() {
        when().get("/hello").then().body(is("Hello Georg!"));
        TEST.modifySourceFile("AppMessages.java", (s -> s.replace("Hello", "Heya")));
        when().get("/hello").then().body(is("Heya Georg!"));

        when().get("/hello/de").then().body(is("Hallo Georg!"));
        TEST.modifyResourceFile("messages/msg_de.properties", (s -> s.replace("Hallo", "Heya")));
        when().get("/hello/de").then().body(is("Heya Georg!"));
    }
}
