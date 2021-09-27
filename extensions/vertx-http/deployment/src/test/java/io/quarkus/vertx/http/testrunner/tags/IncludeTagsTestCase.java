package io.quarkus.vertx.http.testrunner.tags;

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
import io.quarkus.vertx.http.testrunner.HelloResource;

public class IncludeTagsTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(HelloResource.class)
                            .add(new StringAsset(ContinuousTestingTestUtils.appProperties("quarkus.test.include-tags=a")),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClass(TaggedET.class);
                }
            });

    @Test
    public void checkTestsAreRun() throws InterruptedException {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(2L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        test.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return ContinuousTestingTestUtils.appProperties("quarkus.test.include-tags=c");
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        test.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return ContinuousTestingTestUtils.appProperties();
            }
        });
        ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(5L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

    }
}
