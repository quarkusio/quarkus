package io.quarkus.it.spring.web.testprofile;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@EnabledIfSystemProperty(named = "profiles.check", matches = "true")
public class NoQuarkusProfileTest {

    @Test
    public void test() {
        assertNull(System.getProperty("quarkus.test.profile.tags"));
    }
}
