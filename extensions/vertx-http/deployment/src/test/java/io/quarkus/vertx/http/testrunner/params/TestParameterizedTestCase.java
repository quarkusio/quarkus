package io.quarkus.vertx.http.testrunner.params;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

public class TestParameterizedTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(OddResource.class, Setup.class, HelloResource.class)
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties()), "application.properties");
        }
    }).setTestArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClass(ParamET.class);
        }
    });

    public TestParameterizedTestCase() {
        super("devui-continuous-testing");
    }

    @Test
    public void testParameterizedTests() throws InterruptedException, Exception {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(4L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        super.executeJsonRPCMethod("runFailed");

        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(3L, ts.getTestsPassed()); // they are all re-run
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        test.modifyTestSourceFile(ParamET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("4", "3");
            }
        });
        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(5L, ts.getTestsPassed()); // passing test should not have been run
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        Assertions.assertEquals(5L, ts.getTotalTestsPassed());

        test.modifySourceFile(HelloResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("hello", "boo");
            }
        });
        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(4L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

    }
}
