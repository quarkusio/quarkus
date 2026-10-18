package io.quarkus.test.mockito;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

class MockStaticContinuousTestingTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root.addClass(StaticMockService.class)
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties("quarkus.oidc.tenant-enabled=false")),
                            "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(StaticMockET.class));

    @Test
    void staticMockIsClearedBetweenRuns() {
        ContinuousTestingTestUtils testing = new ContinuousTestingTestUtils();
        assertSuccessfulRun(testing.waitForNextCompletion());

        config.modifyTestSourceFile(StaticMockET.class, source -> source.replace("expected-value", "updated-value"));

        assertSuccessfulRun(testing.waitForNextCompletion());
    }

    private static void assertSuccessfulRun(TestStatus status) {
        assertThat(status.getTestsPassed()).isEqualTo(2);
        assertThat(status.getTestsFailed()).isZero();
        assertThat(status.getTestsSkipped()).isZero();
    }
}
