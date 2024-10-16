package io.quarkus.websockets.next.test.upgrade;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.test.utils.WSClient;

public class LocalHttpUpgradeCheckTest extends AbstractHttpUpgradeCheckTestBase {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Opening.class, Responding.class, OpeningHttpUpgradeCheck.class,
                            RejectingHttpUpgradeCheck.class, WSClient.class, Rejecting.class,
                            AlwaysRejectingHttpUpgradeCheck.class, AlwaysInvokedOpeningHttpUpgradeCheck.class));

    @Singleton
    public static final class AlwaysInvokedOpeningHttpUpgradeCheck extends OpeningHttpUpgradeCheck {
        @Override
        protected boolean shouldCheckUpgrade(HttpUpgradeContext context) {
            return true;
        }

        @Override
        public boolean appliesTo(String endpointId) {
            return "opening-id".equals(endpointId);
        }
    }

    @Singleton
    public static final class AlwaysRejectingHttpUpgradeCheck extends RejectingHttpUpgradeCheck {
        @Override
        protected boolean shouldCheckUpgrade(HttpUpgradeContext context) {
            return true;
        }

        @Override
        public boolean appliesTo(String endpointId) {
            return "rejecting-id".equals(endpointId);
        }
    }
}
