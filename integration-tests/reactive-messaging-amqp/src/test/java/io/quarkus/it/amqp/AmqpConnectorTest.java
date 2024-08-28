package io.quarkus.it.amqp;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.awaitility.core.ConditionTimeoutException;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(AmqpBroker.class)
@DisabledOnOs(OS.WINDOWS)
public class AmqpConnectorTest {

    private static final Logger log = Logger.getLogger(AmqpConnectorTest.class);

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };
    protected static final int RETRY_ATTEMPTS = 10;

    @Test
    public void test() {
        for (int i = 0; i < RETRY_ATTEMPTS; i++) {
            try {
                await()
                        .until(() -> get("/amqp/people").as(TYPE_REF).size() >= 6);
                return;
            } catch (ConditionTimeoutException e) {
                log.error("Restarting broker, current payload " + get("/amqp/people").as(TYPE_REF));
                // Sometimes the AMQP broker is just in a broken state, in this case, restart it.
                restartBroker();
            } catch (Exception e) {
                Assertions.fail(e);
                return;
            }
        }
        Assertions.fail("Unable to run the AMQP test successfully, despite " + RETRY_ATTEMPTS + " restarts of the broker");
    }

    private void restartBroker() {
        AmqpBroker.restartBroker();
    }

}
