package io.quarkus.it.profile;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * This test is designed to FAIL during startup (FailingResource throws for LimitProfile).
 * This triggers the cleanup code path in QuarkusTestExtension.
 * <p>
 * The real verification is that BSecondTest and CThirdTestWithDefaultProfile pass,
 * proving cleanup worked correctly - they see app.limit=100 from application.properties,
 * NOT app.limit=3 from this failed profile.
 */
@QuarkusTest
@TestProfile(LimitProfile.class)
@QuarkusTestResource(FailingResource.class)
@Order(1)
class AFirstTest {

    @Test
    void startsWithTheProfile() {
    }
}
