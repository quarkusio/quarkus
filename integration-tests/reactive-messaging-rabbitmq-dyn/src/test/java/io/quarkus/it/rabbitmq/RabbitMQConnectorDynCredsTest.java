package io.quarkus.it.rabbitmq;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.it.rabbitmq.RabbitMQConnectorDynCredsTest.RabbitMQResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(RabbitMQResource.class)
public class RabbitMQConnectorDynCredsTest {

    public static class RabbitMQResource implements QuarkusTestResourceLifecycleManager {

        RabbitMQContainer rabbit;

        @Override
        public Map<String, String> start() {
            String username = "tester";
            String password = RandomStringUtils.insecure().next(10);

            rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"))
                    .withNetwork(Network.SHARED)
                    .withNetworkAliases("rabbitmq")
                    .withUser(username, password)
                    .withPermission("/", username, ".*", ".*", ".*");
            rabbit.start();

            return Map.of(
                    "rabbitmq-host", rabbit.getHost(),
                    "rabbitmq-port", rabbit.getAmqpPort().toString(),
                    "rabbitmq-username", "invalid",
                    "rabbitmq-password", "invalid",
                    "test-creds-provider.username", username,
                    "test-creds-provider.password", password);
        }

        @Override
        public void stop() {
            rabbit.stop();
        }
    }

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void test() {
        await().atMost(30, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(6, get("/rabbitmq/people").as(TYPE_REF).size()));
    }

}
