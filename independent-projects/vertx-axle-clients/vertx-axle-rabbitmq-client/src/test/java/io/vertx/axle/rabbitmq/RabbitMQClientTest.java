package io.vertx.axle.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.axle.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

public class RabbitMQClientTest {

    private static final String QUEUE = "my-queue";
    @Rule
    public GenericContainer container = new FixedHostPortGenericContainer<>("rabbitmq:alpine")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostName("my-rabbit"))
            .withExposedPorts(5672);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() throws KeyManagementException, TimeoutException, NoSuchAlgorithmException, IOException, URISyntaxException {
        String uuid = UUID.randomUUID().toString();
        String uri = "amqp://" + container.getContainerIpAddress() + ":" + container.getMappedPort(5672);
        RabbitMQClient client = RabbitMQClient.create(vertx, new RabbitMQOptions()
                .setUri(uri)
        );
        client.start().toCompletableFuture().join();
        createQueue(uri);
        RabbitMQConsumer consumer = client.basicConsumer(QUEUE).toCompletableFuture().join();
        CompletionStage<Optional<String>> stage = ReactiveStreams.fromPublisher(consumer.toPublisher())
                .map(m -> m.body().toString()).findFirst().run();
        
        client.basicPublish("", QUEUE, new JsonObject().put("body", uuid))
                .toCompletableFuture().join();

        Optional<String> object = stage.toCompletableFuture().join();
        assertThat(object).isNotEmpty().contains(uuid);

        client.stop().toCompletableFuture().join();
    }

    private void createQueue(String uri) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(uri);
        try (Channel channel = factory.newConnection().createChannel()) {
            channel.queueDeclare(QUEUE, true, false, true, null);
        }
    }
}
