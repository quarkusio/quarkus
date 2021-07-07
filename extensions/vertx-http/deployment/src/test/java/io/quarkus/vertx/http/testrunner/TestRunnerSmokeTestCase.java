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
import io.quarkus.vertx.http.deployment.devmode.tests.ClassResult;
import io.quarkus.vertx.http.deployment.devmode.tests.SuiteResult;
import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.restassured.RestAssured;

public class TestRunnerSmokeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class, UnitService.class)
                            .add(new StringAsset(ContinuousTestingTestUtils.appProperties()),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(SimpleET.class, UnitET.class);
                }
            });

    @Test
    public void checkTestsAreRun() throws InterruptedException {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();
        ;

        Assertions.assertEquals(3L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(3L, ts.getTotalTestsFailed());
        Assertions.assertEquals(1L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        SuiteResult suiteResult = RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/tests/result")
                .as(SuiteResult.class);
        Assertions.assertEquals(2, suiteResult.getResults().size());
        for (ClassResult cr : suiteResult.getResults().values()) {
            if (cr.getClassName().equals(SimpleET.class.getName())) {
                Assertions.assertEquals(1, cr.getFailing().size());
                Assertions.assertEquals(1, cr.getPassing().size());
            } else if (cr.getClassName().equals(UnitET.class.getName())) {
                Assertions.assertEquals(2, cr.getFailing().size());
                Assertions.assertEquals(0, cr.getPassing().size());
            } else {
                Assertions.fail("Unexpected test " + cr.getClassName());
            }
        }

        //now add the functionality
        test.modifySourceFile(HelloResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//setup(router);", "setup(router);");
            }
        });
        ts = utils.waitForNextCompletion();
        ;

        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(2L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        //fix the unit test

        test.modifySourceFile(UnitService.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("unit", "UNIT");
            }
        });
        ts = utils.waitForNextCompletion();
        ;

        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(1L, ts.getTotalTestsFailed());
        Assertions.assertEquals(3L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        test.modifyTestSourceFile(UnitET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("Hi", "hello");
            }
        });
        ts = utils.waitForNextCompletion();
        ;

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(4L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        //disable the unit test
        test.modifyTestSourceFile(UnitET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("@Test", "@Test @org.junit.jupiter.api.Disabled");
            }
        });
        ts = utils.waitForNextCompletion();
        ;

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(2L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(2L, ts.getTotalTestsSkipped());

        //delete the unit test
        test.modifyTestSourceFile(UnitET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("@Test", "//@Test");
            }
        });
        ts = utils.waitForNextCompletion();
        ;

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

    }
}
