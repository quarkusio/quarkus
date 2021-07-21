package io.quarkus.vertx.http.testrunner;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;

public class TestFailingBeforeAllTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class)
                            .add(new StringAsset(ContinuousTestingTestUtils.appProperties()),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(SimpleET.class);
                }
            });

    @Test
    public void testBrokenBeforeAllHandling() throws InterruptedException {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(1L, ts.getTotalTestsFailed());
        Assertions.assertEquals(1L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        test.modifyTestSourceFile(SimpleET.class, s -> s.replaceFirst("\\{", "{ \n" +
                "    @org.junit.jupiter.api.BeforeAll public static void error() { throw new RuntimeException();  }"));

        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(2L, ts.getTotalTestsFailed());
        Assertions.assertEquals(0L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

    }
}
