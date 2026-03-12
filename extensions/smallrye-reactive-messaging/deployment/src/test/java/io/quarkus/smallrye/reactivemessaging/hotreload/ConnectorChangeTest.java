package io.quarkus.smallrye.reactivemessaging.hotreload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.RepeatedTest;
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

    @RepeatedTest(50)
    //    @Test
    public void testUpdatingConnector() {
        // Wait for initial stream to complete - may lose 1-2 messages during startup
        await().untilAsserted(() -> assertThat(get())
                .hasSizeGreaterThanOrEqualTo(7) // Allow for 1-2 message loss
                .allMatch(s -> List.of("-2", "-3", "-4", "-5", "-6", "-7", "-8", "-9", "-10", "-11").contains(s),
                        "All values should match expected increment=1"));
        reset();

        // Update processor
        TEST.modifySourceFile("SomeConnector.java", s -> s.replace(".orElse(1)", ".orElse(2)"));
        await().untilAsserted(() -> assertThat(get())
                .hasSizeGreaterThanOrEqualTo(7) // Allow for 1-2 message loss during reload
                .allMatch(s -> List.of("-3", "-4", "-5", "-6", "-7", "-8", "-9", "-10", "-11").contains(s),
                        "All values should match expected increment=2"));
        reset();

        // Update source
        TEST.modifyResourceFile("application.properties", s -> s.concat("mp.messaging.incoming.my-source.increment=5"));
        await().untilAsserted(() -> assertThat(get())
                .hasSizeGreaterThanOrEqualTo(7) // Allow for 1-2 message loss during reload
                .allMatch(s -> List.of("-6", "-7", "-8", "-9", "-10", "-11", "-12", "-13", "-14").contains(s),
                        "All values should match expected increment=5"));
        reset();
    }

    static JsonArray get() {
        return new JsonArray(RestAssured.get().asString());
    }

    static void reset() {
        RestAssured.get("/reset");
    }

}
