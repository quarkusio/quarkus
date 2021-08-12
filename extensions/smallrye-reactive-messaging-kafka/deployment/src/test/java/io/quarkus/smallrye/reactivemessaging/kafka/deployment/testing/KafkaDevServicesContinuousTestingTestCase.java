package io.quarkus.smallrye.reactivemessaging.kafka.deployment.testing;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.testrunner.ContinuousTestingTestUtils;

public class KafkaDevServicesContinuousTestingTestCase {

    static final String FINAL_APP_PROPERTIES = ContinuousTestingTestUtils.appProperties(
            "mp.messaging.outgoing.generated-price.connector=smallrye-kafka",
            "mp.messaging.outgoing.generated-price.topic=prices",
            "mp.messaging.outgoing.generated-price.value.serializer=org.apache.kafka.common.serialization.IntegerSerializer",
            "mp.messaging.incoming.prices.connector=smallrye-kafka",
            "mp.messaging.incoming.prices.health-readiness-enabled=false",
            "mp.messaging.incoming.prices.topic=prices",
            "mp.messaging.incoming.prices.value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer");

    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(PriceConverter.class, PriceResource.class, PriceGenerator.class)
                            .addAsResource(new StringAsset(ContinuousTestingTestUtils.appProperties("")),
                                    "application.properties");
                }
            }).setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(PriceResourceET.class);
                }
            });

    //see https://github.com/quarkusio/quarkus/issues/19180
    @Test
    public void testContinuousTestingScenario1() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifySourceFile(PriceResource.class, s -> s.replaceAll("//", ""));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifySourceFile(PriceConverter.class, s -> s.replaceAll("//", ""));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifySourceFile(PriceGenerator.class, s -> s.replaceAll("//", ""));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifyResourceFile("application.properties", s -> FINAL_APP_PROPERTIES);
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());
    }

    @Test
    public void testContinuousTestingScenario2() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifyResourceFile("application.properties", s -> FINAL_APP_PROPERTIES);
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifySourceFile(PriceConverter.class, s -> s.replaceAll("//", ""));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifySourceFile(PriceGenerator.class, s -> s.replaceAll("//", ""));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifySourceFile(PriceResource.class, s -> s.replaceAll("//", ""));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());
    }
}
