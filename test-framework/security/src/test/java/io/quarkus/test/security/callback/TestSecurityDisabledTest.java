package io.quarkus.test.security.callback;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TestSecurityDisabledTest extends AbstractSecurityCallbackTest {

    @BeforeAll
    public static void checkBeforeAll() {
        // this is called before QuarkusSecurityTestExtension.beforeEach, so testIdentity should still be null
        assertTestIdentityIsNull();
    }

    @BeforeEach
    public void checkBeforeEach() {
        // this is called after QuarkusSecurityTestExtension.beforeEach, with no @TestSecurity, this should be still null
        assertTestIdentityIsNull();
    }

    @Test
    public void checkTest() {
        // QuarkusSecurityTestExtension.beforeEach should not set test identity
        assertTestIdentityIsNull();

        // set testIdentity, so we can later check, if it is set to null or not
        setTestIdentityToValue();
    }

    @AfterEach
    public void checkAfterEach() {
        // testIdentity was set in test, it should be still set
        assertTestIdentityIsNotNull();
    }

    @AfterAll
    public static void checkAfterAll() {
        // testIdentity was set in test, it should be still set
        assertTestIdentityIsNotNull();

        // reset testIdentity to null, so it won't break other tests
        setTestIdentityToNull();
    }
}
