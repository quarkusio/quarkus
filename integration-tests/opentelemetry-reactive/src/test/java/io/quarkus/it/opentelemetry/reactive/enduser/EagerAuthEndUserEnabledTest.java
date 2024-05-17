package io.quarkus.it.opentelemetry.reactive.enduser;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(EndUserProfile.class)
public class EagerAuthEndUserEnabledTest extends AbstractEndUserTest {

    @Override
    protected boolean isProactiveAuthEnabled() {
        return true;
    }

}
