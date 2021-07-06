package io.quarkus.vertx.http.testrunner.params;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.quarkus.vertx.http.testrunner.ContinuousTestingTestUtils;

public class TestParameterizedTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(OddResource.class, Setup.class, HelloResource.class)
                            .add(new StringAsset(ContinuousTestingTestUtils.appProperties()),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(ParamET.class);
                }
            });

    @Test
    public void testParameterizedTests() throws InterruptedException {
        TestStatus ts = ContinuousTestingTestUtils.waitForFirstRunToComplete();
        Assertions.assertEquals(1L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(4L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        test.modifyTestSourceFile(ParamET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("4", "3");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(2);
        Assertions.assertEquals(2L, ts.getLastRun());
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(5L, ts.getTestsPassed()); //passing test should not have been run
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(5L, ts.getTotalTestsPassed());

        test.modifySourceFile(HelloResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("hello", "boo");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(3);
        Assertions.assertEquals(3L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(4L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

    }
}
