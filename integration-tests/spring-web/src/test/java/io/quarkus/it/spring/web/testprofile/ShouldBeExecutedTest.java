package io.quarkus.it.spring.web.testprofile;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(Profiles.QuarkusTestProfileMatchingTag.class)
@EnabledIfSystemProperty(named = "profiles.check", matches = "true")
public class ShouldBeExecutedTest {

    @Test
    public void test() {
        assertEquals(1, Profiles.QuarkusTestProfileMatchingTag.DummyTestResource.COUNT.intValue());
        assertTrue(System.getProperty("quarkus.test.profile.tags").contains("test1"));
    }

}
