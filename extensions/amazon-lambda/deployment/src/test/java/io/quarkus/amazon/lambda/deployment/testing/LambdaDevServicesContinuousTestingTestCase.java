package io.quarkus.amazon.lambda.deployment.testing;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;

public class LambdaDevServicesContinuousTestingTestCase {
    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GreetingLambda.class, Person.class)
                            .addAsResource(new StringAsset(ContinuousTestingTestUtils.appProperties("")),
                                    "application.properties");
                }
            }).setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(LambdaHandlerET.class);
                }
            });

    @Test
    public void testLambda() throws Exception {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());
        test.modifySourceFile(GreetingLambda.class, s -> s.replace("Hey", "Yo"));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifyTestSourceFile(LambdaHandlerET.class, s -> s.replace("Hey", "Yo"));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());

    }
}
