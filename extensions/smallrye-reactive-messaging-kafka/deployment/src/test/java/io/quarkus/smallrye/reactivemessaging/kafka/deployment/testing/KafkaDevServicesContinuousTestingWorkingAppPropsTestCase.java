package io.quarkus.smallrye.reactivemessaging.kafka.deployment.testing;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;

public class KafkaDevServicesContinuousTestingWorkingAppPropsTestCase {

    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(PriceConverter.class, PriceResource.class, PriceGenerator.class)
                            .addAsResource(new StringAsset(KafkaDevServicesContinuousTestingTestCase.FINAL_APP_PROPERTIES),
                                    "application.properties");
                }
            }).setTestArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(PriceResourceET.class);
                }
            });

    /**
     * Similar to {@link KafkaDevServicesContinuousTestingTestCase} however it starts with application.properties configured.
     *
     * See https://github.com/quarkusio/quarkus/issues/19180.
     */
    @Test
    @Disabled("flaky")
    public void testContinuousTestingScenario3() {
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
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());
    }

}
