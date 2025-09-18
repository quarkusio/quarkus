package io.quarkus.it.kafka.continuoustesting;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.it.kafka.BundledEndpoint;
import io.quarkus.it.kafka.KafkaAdminManager;
import io.quarkus.it.kafka.KafkaAdminTest;
import io.quarkus.it.kafka.KafkaEndpoint;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.QuarkusDevModeTest;
import io.strimzi.test.container.StrimziKafkaContainer;

public class DevServicesContainerSharingTest extends BaseDevServiceTest {

    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .deleteClass(KafkaEndpoint.class)
                    .addClass(BundledEndpoint.class)
                    .addClass(KafkaAdminManager.class))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(KafkaAdminTest.class));

    static StrimziKafkaContainer kafka;

    @BeforeAll
    static void beforeAll() {
        kafka = new StrimziKafkaContainer().withLabel("quarkus-dev-service-kafka", "kafka");
        kafka.start();
    }

    @AfterAll
    static void afterAll() {
        if (kafka != null) {
            kafka.stop();
            kafka = null;
        }
    }

    @Test
    void test() {
        ping();
        assertTrue(getKafkaContainers(LaunchMode.DEVELOPMENT).isEmpty());
    }

    void ping() {
        when().get("/kafka/partitions/test").then()
                .statusCode(200)
                .body(is("2"));
    }
}
