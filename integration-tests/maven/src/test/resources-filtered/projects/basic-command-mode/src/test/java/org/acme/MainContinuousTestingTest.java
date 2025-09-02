package org.acme;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

@Disabled("Fails due to 'More than one @QuarkusMain method found with name '': org.acme.Main and org.acme.Main'")
public class MainContinuousTestingTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClass(Main.class)
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties()), "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MainTest.class));

    @Test
    void testContinuousTesting() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();

        TestStatus ts = utils.waitForNextCompletion();
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
    }
}
