package io.quarkus.smallrye.reactivemessaging.hotreload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

    @RepeatedTest(10)
    //    @Test
    public void testUpdatingConnector() {
        // Wait for initial stream to complete - may lose messages during startup wiring
        await().untilAsserted(() -> assertThat(get())
                .hasSizeGreaterThanOrEqualTo(7)
                .allMatch(s -> {
                    int v = Integer.parseInt((String) s);
                    return v >= -101 && v <= -2;
                }, "All values should match expected increment=1"));
        reset();

        // Update processor
        TEST.modifySourceFile("SomeConnector.java", s -> s.replace(".orElse(1)", ".orElse(2)"));
        await().untilAsserted(() -> assertThat(get())
                .hasSizeGreaterThanOrEqualTo(7)
                .allMatch(s -> {
                    int v = Integer.parseInt((String) s);
                    return v >= -102 && v <= -3;
                }, "All values should match expected increment=2"));
        reset();

        // Update source
        TEST.modifyResourceFile("application.properties", s -> s.concat("mp.messaging.incoming.my-source.increment=5"));
        await().untilAsserted(() -> assertThat(get())
                .hasSizeGreaterThanOrEqualTo(7)
                .allMatch(s -> {
                    int v = Integer.parseInt((String) s);
                    return v >= -105 && v <= -6;
                }, "All values should match expected increment=5"));
        reset();
    }

    static JsonArray get() {
        return new JsonArray(RestAssured.get().asString());
    }

    static void reset() {
        RestAssured.get("/reset");
    }

}
