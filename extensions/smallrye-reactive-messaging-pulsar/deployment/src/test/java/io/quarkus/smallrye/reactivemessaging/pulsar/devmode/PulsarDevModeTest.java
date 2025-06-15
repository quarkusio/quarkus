package io.quarkus.smallrye.reactivemessaging.pulsar.devmode;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.pulsar.ConsumingBean;
import io.quarkus.smallrye.reactivemessaging.pulsar.ProducingBean;
import io.quarkus.smallrye.reactivemessaging.pulsar.TestResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class PulsarDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConsumingBean.class, ProducingBean.class, TestResource.class)
                    .addAsResource("application.properties"));

    @Test
    public void testCodeUpdate() {
        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            String value = RestAssured.get("/last").asString();
            return value.equalsIgnoreCase("20");
        });

        TEST.modifySourceFile(ProducingBean.class, s -> s.replace("* 2", "* 3"));

        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            String value = RestAssured.get("/last").asString();
            return value.equalsIgnoreCase("30");
        });

    }

}
