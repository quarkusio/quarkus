package io.quarkus.smallrye.reactivemessaging.hotreload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;

public class ConnectorChangeTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SomeConnector.class, SomeProcessor.class)
                            .addAsResource(new StringAsset(
                                    "mp.messaging.incoming.my-source.connector=quarkus-test-connector\n"
                                            + "mp.messaging.outgoing.my-sink.connector=quarkus-test-connector\n"),
                                    "application.properties"));

    @Test
    public void testUpdatingConnector() {
        await().until(() -> get().size() > 5);
        assertThat(get()).startsWith("-2", "-3", "-4", "-5");

        // Update processor
        TEST.modifySourceFile("SomeConnector.java", s -> s.replace(".orElse(1)", ".orElse(2)"));
        await().until(() -> get().size() > 5);
        assertThat(get()).startsWith("-3", "-4", "-5", "-6");

        // Update source
        TEST.modifyResourceFile("application.properties", s -> s.concat("mp.messaging.incoming.my-source.increment=5"));
        await().until(() -> get().size() > 5);
        assertThat(get()).startsWith("-6", "-7", "-8", "-9");
    }

    static JsonArray get() {
        return new JsonArray(RestAssured.get().asString());
    }

}
