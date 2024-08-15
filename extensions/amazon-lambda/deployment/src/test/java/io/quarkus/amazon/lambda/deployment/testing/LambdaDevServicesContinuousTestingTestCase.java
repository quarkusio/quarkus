package io.quarkus.amazon.lambda.deployment.testing;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.amazon.lambda.deployment.testing.model.OutputPerson;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;

@Disabled("https://github.com/quarkusio/quarkus/issues/33963")
public class LambdaDevServicesContinuousTestingTestCase {
    @RegisterExtension
    public static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GreetingLambda.class, InputPerson.class, OutputPerson.class)
                            .addAsResource(
                                    new StringAsset(ContinuousTestingTestUtils.appProperties(
                                            "quarkus.log.category.\"io.quarkus.amazon.lambda.runtime\".level=DEBUG")),
                                    "application.properties");
                }
            }).setTestArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(GreetingLambdaTest.class);
                }
            });

    //run this twice, to make sure everything is cleaned up properly
    @RepeatedTest(2)
    public void testLambda() throws Exception {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        var result = utils.waitForNextCompletion();
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());
        test.modifySourceFile(GreetingLambda.class, s -> s.replace("Hey", "Yo"));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(0, result.getTotalTestsPassed());
        Assertions.assertEquals(1, result.getTotalTestsFailed());
        test.modifyTestSourceFile(GreetingLambdaTest.class, s -> s.replace("Hey", "Yo"));
        result = utils.waitForNextCompletion();
        Assertions.assertEquals(1, result.getTotalTestsPassed());
        Assertions.assertEquals(0, result.getTotalTestsFailed());

    }
}
