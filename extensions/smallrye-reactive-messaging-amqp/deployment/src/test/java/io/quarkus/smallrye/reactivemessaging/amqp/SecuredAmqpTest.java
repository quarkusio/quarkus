package io.quarkus.smallrye.reactivemessaging.amqp;

import static org.awaitility.Awaitility.await;

import org.apache.activemq.artemis.protocol.amqp.broker.ProtonProtocolManagerFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SecuredAmqpTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConsumingBean.class, ProducingBean.class, TestResource.class,
                            SecuredAmqpBroker.class, ProtonProtocolManagerFactory.class)
                    .addAsResource("broker.xml"))
            .setBeforeAllCustomizer(SecuredAmqpBroker::start)
            .setAfterAllCustomizer(SecuredAmqpBroker::stop)
            .withConfigurationResource("application-secured.properties");

    @Test
    public void test() {
        await().until(() -> {
            String value = RestAssured.get("/last").asString();
            return value.equalsIgnoreCase("20");
        });
    }
}
