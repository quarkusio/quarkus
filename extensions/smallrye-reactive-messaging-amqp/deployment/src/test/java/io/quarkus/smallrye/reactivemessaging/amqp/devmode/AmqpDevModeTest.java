package io.quarkus.smallrye.reactivemessaging.amqp.devmode;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.protocol.amqp.broker.ProtonProtocolManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.amqp.AnonymousAmqpBroker;
import io.quarkus.smallrye.reactivemessaging.amqp.ConsumingBean;
import io.quarkus.smallrye.reactivemessaging.amqp.ProducingBean;
import io.quarkus.smallrye.reactivemessaging.amqp.TestResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class AmqpDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConsumingBean.class, ProducingBean.class, TestResource.class,
                            AnonymousAmqpBroker.class, ProtonProtocolManagerFactory.class)
                    .addAsResource("broker.xml")
                    .addAsResource("application.properties"));

    @BeforeAll
    public static void startBroker() {
        AnonymousAmqpBroker.start();
    }

    @AfterAll
    public static void stopBroker() {
        AnonymousAmqpBroker.stop();
    }

    @Test
    public void testCodeUpdate() {
        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    String value = RestAssured.get("/last").asString();
                    return value.equalsIgnoreCase("20");
                });

        TEST.modifySourceFile(ProducingBean.class, s -> s.replace("* 2", "* 3"));

        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    String value = RestAssured.get("/last").asString();
                    return value.equalsIgnoreCase("30");
                });

    }

}
