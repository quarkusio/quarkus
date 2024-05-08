package io.quarkus.it.opentelemetry;

import io.quarkus.it.opentelemetry.util.LazyAuthEndUserProfile;
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
