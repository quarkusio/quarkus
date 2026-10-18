package io.quarkus.it.spring.web.testprofile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(Profiles.QuarkusTestProfileNonMatchingTag.class)
@EnabledIfSystemProperty(named = "profiles.check", matches = "true")
public class ShouldAlsoNeverBeExecutedQuarkusTestProfileTest {

    public ShouldAlsoNeverBeExecutedQuarkusTestProfileTest() {
        throw new IllegalStateException("Should not have executed");
    }

    @Test
    public void test() {
        Assertions.fail("Should not have executed");
    }
}
