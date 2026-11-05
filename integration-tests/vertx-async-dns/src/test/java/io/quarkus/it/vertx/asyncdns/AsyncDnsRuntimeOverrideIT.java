package io.quarkus.it.vertx.asyncdns;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@WithTestResource(value = AsyncDnsRuntimeOverrideIT.RuntimeOverride.class, scope = TestResourceScope.RESTRICTED_TO_CLASS)
public class AsyncDnsRuntimeOverrideIT extends AsyncDnsTest {

    public static class RuntimeOverride implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            // The native executable must retain its build-time resolver choice.
            return Map.of("quarkus.vertx.use-async-dns", "false",
                    "quarkus.config.build-time-mismatch-at-runtime", "warn");
        }

        @Override
        public void stop() {
        }
    }
}
