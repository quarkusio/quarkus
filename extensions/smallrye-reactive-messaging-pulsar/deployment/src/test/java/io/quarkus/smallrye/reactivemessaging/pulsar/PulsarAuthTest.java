package io.quarkus.smallrye.reactivemessaging.pulsar;

import static org.awaitility.Awaitility.await;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PulsarAuthTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(ConsumingBean.class,
                    ProducingBean.class, TestResource.class))
            .withConfigurationResource("application-secured.properties");

    @Test
    public void test() {
        await().until(() -> {
            String value = RestAssured.get("/last").asString();
            return value.equalsIgnoreCase("20");
        });
    }
}
