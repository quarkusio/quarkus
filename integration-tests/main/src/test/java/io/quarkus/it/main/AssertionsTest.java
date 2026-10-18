package io.quarkus.it.main;

import static org.assertj.core.api.Fail.fail;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AssertionsTest {

    @Test
    void assertionsShouldBeEnabled() {
        try {
            assert 1 == 2;
        } catch (AssertionError expected) {
            return;
        }
        fail("No AssertionError was thrown on a failed assertion");
    }
}
