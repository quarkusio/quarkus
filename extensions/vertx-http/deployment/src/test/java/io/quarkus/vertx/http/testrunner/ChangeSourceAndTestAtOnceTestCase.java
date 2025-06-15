package io.quarkus.vertx.http.testrunner;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.dev.testing.TestScanningLock;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

public class ChangeSourceAndTestAtOnceTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(CoupledService.class).add(
                    new StringAsset(ContinuousTestingTestUtils.appProperties("quarkus.test.type=unit")),
                    "application.properties");
        }
    }).setTestArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(CoupledET.class);
        }
    });

    @Test
    public void testChangeSourceAndTestAtOnce() throws InterruptedException {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();
        Assertions.assertEquals(1L, ts.getTestsPassed());
        TestScanningLock.lockForTests();
        try {
            test.modifySourceFile(CoupledService.class, new Function<String, String>() {
                @Override
                public String apply(String s) {
                    return s.replace("service", "newService");
                }
            });
            test.modifyTestSourceFile(CoupledET.class, new Function<String, String>() {
                @Override
                public String apply(String s) {
                    return s.replace("service", "newService");
                }
            });

        } finally {
            TestScanningLock.unlockForTests();
        }
        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(1L, ts.getTestsPassed());

    }
}
