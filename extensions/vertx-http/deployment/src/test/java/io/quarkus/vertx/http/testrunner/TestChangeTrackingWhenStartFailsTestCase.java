package io.quarkus.vertx.http.testrunner;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

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
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();
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
        ts = utils.waitForNextCompletion();
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
        ts = utils.waitForNextCompletion();
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
        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

    }
}
