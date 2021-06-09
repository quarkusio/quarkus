package io.quarkus.vertx.http.testrunner.brokenonly;

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
import io.restassured.RestAssured;

public class TestBrokenOnlyTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(BrokenOnlyResource.class)
                            .add(new StringAsset(ContinuousTestingTestUtils.appProperties()),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(SimpleET.class);
                }
            });

    @Test
    public void testBrokenOnlyMode() throws InterruptedException {
        TestStatus ts = ContinuousTestingTestUtils.waitForFirstRunToComplete();
        Assertions.assertEquals(1L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(-1L, ts.getRunning());

        //start broken only mode
        RestAssured.post("q/dev/io.quarkus.quarkus-vertx-http/tests/toggle-broken-only");

        test.modifyTestSourceFile(SimpleET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("@QuarkusTest", "@QuarkusTest //noop change");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(2);
        Assertions.assertEquals(2L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed()); //passing test should not have been run
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(-1L, ts.getRunning());

        test.modifySourceFile(BrokenOnlyResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//setup(router);", "setup(router);");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(3);
        Assertions.assertEquals(3L, ts.getLastRun());
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(-1L, ts.getRunning());

        //now add a new failing test
        test.modifyTestSourceFile(SimpleET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//failannotation", "@Test");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(4);
        Assertions.assertEquals(4L, ts.getLastRun());
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(-1L, ts.getRunning());

        //now make it pass
        test.modifyTestSourceFile(SimpleET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("Assertions.fail();", "//noop");
            }
        });
        ts = ContinuousTestingTestUtils.waitForRun(5);
        Assertions.assertEquals(5L, ts.getLastRun());
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(-1L, ts.getRunning());

    }
}
