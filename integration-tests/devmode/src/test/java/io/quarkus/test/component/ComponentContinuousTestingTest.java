package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

public class ComponentContinuousTestingTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(
                    root -> root.addClass(ComponentFoo.class).add(new StringAsset(ContinuousTestingTestUtils.appProperties()),
                            "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ComponentUT.class));

    @Test
    public void test() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        TestStatus ts = utils.waitForNextCompletion();
        assertEquals(0L, ts.getTestsFailed());
        assertEquals(1L, ts.getTestsPassed());
        assertEquals(0L, ts.getTestsSkipped());

        config.modifySourceFile(ComponentFoo.class, s -> s.replace("return bar;", "return bar + bar;"));

        ts = utils.waitForNextCompletion();
        assertEquals(1L, ts.getTestsFailed());
        assertEquals(0L, ts.getTestsPassed());
        assertEquals(0L, ts.getTestsSkipped());
    }

}
