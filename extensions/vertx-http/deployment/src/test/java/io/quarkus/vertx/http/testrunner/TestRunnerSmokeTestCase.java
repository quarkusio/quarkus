package io.quarkus.vertx.http.testrunner;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

public class TestRunnerSmokeTestCase extends DevUIJsonRPCTest {
    ObjectMapper mapper = new ObjectMapper();

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class, UnitService.class)
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties()), "application.properties");
        }
    }).setTestArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(SimpleET.class, UnitET.class);
        }
    });

    public TestRunnerSmokeTestCase() {
        super("devui-continuous-testing");
    }

    @Test
    public void checkTestsAreRun() throws InterruptedException, Exception {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(3L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(3L, ts.getTotalTestsFailed());
        Assertions.assertEquals(1L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        JsonNode jsonRPCResult = super.executeJsonRPCMethod("getResults");
        Assertions.assertNotNull(jsonRPCResult);
        Assertions.assertTrue(jsonRPCResult.has("results"));

        JsonNode results = jsonRPCResult.get("results");
        Assertions.assertNotNull(results);

        Iterator<Map.Entry<String, JsonNode>> fields = results.fields();

        while (fields.hasNext()) {
            JsonNode testResult = fields.next().getValue();
            String className = testResult.get("className").asText();

            JsonNode passing = testResult.get("passing");
            JsonNode failing = testResult.get("failing");

            if (className.equals(SimpleET.class.getName())) {
                Assertions.assertEquals(1, failing.size(), className);
                Assertions.assertEquals(2, passing.size(), className);
            } else if (className.equals(UnitET.class.getName())) {
                Assertions.assertEquals(2, failing.size(), className);
                Assertions.assertEquals(1, passing.size(), className);
            } else {
                Assertions.fail("Unexpected test " + className);
            }
        }

        // now add the functionality
        test.modifySourceFile(HelloResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("//setup(router);", "setup(router);");
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(2L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        // fix the unit test

        test.modifySourceFile(UnitService.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("unit", "UNIT");
            }
        });
        ts = utils.waitForNextCompletion();

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

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(4L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        // disable the unit test
        test.modifyTestSourceFile(UnitET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("@Test", "@Test @org.junit.jupiter.api.Disabled");
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(2L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(2L, ts.getTotalTestsSkipped());

        // delete the unit test
        test.modifyTestSourceFile(UnitET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("@Test", "//@Test");
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        // now test compile errors
        test.modifySourceFile(HelloResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("\"hello", "\"hello\" world");
            }
        });
        // we just sleep here
        Thread.sleep(1000);
        test.modifySourceFile(HelloResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("\"hello\" world", "\"hello world");
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(2L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(2L, ts.getTotalTestsFailed());
        Assertions.assertEquals(0L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

        // now test compile errors for the test itself
        test.modifyTestSourceFile(SimpleET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("\"hello", "\"hello\" world");
            }
        });
        // we just sleep here
        Thread.sleep(1000);
        test.modifyTestSourceFile(SimpleET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("\"hello\" world", "\"hello world");
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
        Assertions.assertEquals(0L, ts.getTotalTestsFailed());
        Assertions.assertEquals(2L, ts.getTotalTestsPassed());
        Assertions.assertEquals(0L, ts.getTotalTestsSkipped());

    }
}
