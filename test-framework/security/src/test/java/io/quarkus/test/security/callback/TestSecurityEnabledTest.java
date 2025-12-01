package io.quarkus.test.security.callback;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class TestSecurityEnabledTest extends AbstractSecurityCallbackTest {
    @BeforeAll
    public static void checkBeforeAll() {
        // this is called before QuarkusSecurityTestExtension.beforeEach, so testIdentity should still be null
        assertTestIdentityIsNull();
    }

    @BeforeEach
    public void checkBeforeEach() {
        // this is called after QuarkusSecurityTestExtension.beforeEach, so testIdentity should be set
        assertTestIdentityIsNotNull();
    }

    @Test
    @TestSecurity(user = "myUser")
    public void checkTestItself() {
        // QuarkusSecurityTestExtension.beforeEach should set test identity
        assertTestIdentityIsNotNull();
    }

    @AfterAll
    public static void checkAfterAll() {
        // this is called after QuarkusSecurityTestExtension.afterEach, so testIdentity should be set to null again
        assertTestIdentityIsNull();
    }
}
