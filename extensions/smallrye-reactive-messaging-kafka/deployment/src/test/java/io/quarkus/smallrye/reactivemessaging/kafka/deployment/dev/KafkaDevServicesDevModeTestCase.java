package io.quarkus.smallrye.reactivemessaging.kafka.deployment.dev;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class KafkaDevServicesDevModeTestCase {

    static final String FINAL_APP_PROPERTIES = "mp.messaging.outgoing.generated-price.connector=smallrye-kafka\n" +
            "mp.messaging.outgoing.generated-price.topic=prices\n" +
            "mp.messaging.incoming.prices.connector=smallrye-kafka\n" +
            "mp.messaging.incoming.prices.health-readiness-enabled=false\n" +
            "mp.messaging.incoming.prices.topic=prices\n";

    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(PriceConverter.class, PriceResource.class, PriceGenerator.class)
                            .addAsResource(new StringAsset(FINAL_APP_PROPERTIES),
                                    "application.properties");
                }
            });

    @TestHTTPResource("/prices/stream")
    URI uri;

    @Test
    public void sseStream() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(this.uri);

        List<Double> received = new CopyOnWriteArrayList<>();

        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(inboundSseEvent -> received.add(Double.valueOf(inboundSseEvent.readData())));
            source.open();

            Awaitility.await()
                    .atMost(Duration.ofSeconds(1))
                    .until(() -> received.size() >= 2);
        }

        Assertions.assertThat(received)
                .hasSizeGreaterThanOrEqualTo(2)
                .allMatch(value -> (value >= 0) && (value < 100));

        test.modifySourceFile(PriceConverter.class, s -> s.replace("int ", "long "));
        test.modifySourceFile(PriceGenerator.class,
                s -> s.replace("Integer", "Long").replace("this.random", "(long)this.random"));

        received.clear();

        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(inboundSseEvent -> received.add(Double.valueOf(inboundSseEvent.readData())));
            source.open();

            Awaitility.await()
                    .atMost(Duration.ofSeconds(3))
                    .until(() -> received.size() >= 2);
        }

        Assertions.assertThat(received)
                .hasSizeGreaterThanOrEqualTo(2)
                .allMatch(value -> (value >= 0) && (value < 100));
    }
}
