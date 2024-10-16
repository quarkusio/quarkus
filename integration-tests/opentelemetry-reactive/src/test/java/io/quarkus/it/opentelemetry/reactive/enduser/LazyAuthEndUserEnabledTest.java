package io.quarkus.it.opentelemetry.reactive.enduser;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(LazyAuthEndUserProfile.class)
public class LazyAuthEndUserEnabledTest extends AbstractEndUserTest {
    @Override
    protected boolean isProactiveAuthEnabled() {
        return false;
    }
}
