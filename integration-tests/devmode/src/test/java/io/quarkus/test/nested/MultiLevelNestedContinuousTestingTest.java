package io.quarkus.test.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

public class MultiLevelNestedContinuousTestingTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(
                    root -> root.addClass(NestedBean.class)
                            .add(new StringAsset(ContinuousTestingTestUtils.appProperties(
                                    "quarkus.oidc.tenant-enabled=false")),
                                    "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MultiLevelNestedUT.class));

    @Test
    public void testMultiLevelNestedClassesInContinuousTesting() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();
        assertEquals(0L, ts.getTestsFailed());
        assertEquals(1L, ts.getTestsPassed());
        assertEquals(0L, ts.getTestsSkipped());
    }
}
