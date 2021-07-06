package io.quarkus.vertx.http.testrunner;

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

public class TestChangeTrackingWhenStartFailsTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class, StartupFailer.class).add(
                            new StringAsset(ContinuousTestingTestUtils.appProperties()),
                            "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(SimpleET.class, DuplicateSimpleET.class);
                }
            });

    @Test
    public void testChangeTrackingOnStartupFailure() throws InterruptedException {
        TestStatus ts = ContinuousTestingTestUtils.waitForFirstRunToComplete();
        Assertions.assertEquals(1L, ts.getLastRun());
        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        //fail the startup
        test.modifySourceFile(StartupFailer.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//fail();", "fail();");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(2);
        Assertions.assertEquals(2L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(3L, ts.getTestsSkipped());
        //fail again
        test.modifySourceFile(StartupFailer.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("fail();", "fail();fail();");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(3);
        Assertions.assertEquals(3L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(3L, ts.getTestsSkipped());
        //now lets pass
        test.modifySourceFile(StartupFailer.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("fail();fail();", "//fail();");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(4);
        Assertions.assertEquals(4L, ts.getLastRun());
        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
    }
}
