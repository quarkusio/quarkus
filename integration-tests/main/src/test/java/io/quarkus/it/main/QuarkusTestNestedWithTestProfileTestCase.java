package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@Tag("nested")
@TestProfile(QuarkusTestNestedWithTestProfileTestCase.OuterProfile.class)
public class QuarkusTestNestedWithTestProfileTestCase {

    private static final int TEST_PORT_FROM_PROFILE = 7777;

    @Nested
    class NestedCase {

        @Test
        void testProfileFromNested() {
            Assertions.assertEquals(TEST_PORT_FROM_PROFILE, RestAssured.port);
            RestAssured.when()
                    .get("/greeting/Stu")
                    .then()
                    .statusCode(200)
                    .body(is("OuterProfile Stu"));
        }
    }

    @Nested
    @TestProfile(QuarkusTestNestedWithTestProfileTestCase.ModernEnglishProfile.class)
    @Disabled("With the current test classloading design, test profiles on nested inner classes are too hard, because nested classes should run with the same classloader as the parent, but a test profile involves a new application, which involves a new classloader.")
    class ModernEnglishCase {

        @Test
        void testProfileFromNested() {
            RestAssured.when()
                    .get("/greeting/Stu")
                    .then()
                    .statusCode(200)
                    .body(is("Hey Stu"));
        }
    }

    public static class OuterProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("quarkus.http.test-port", "" + TEST_PORT_FROM_PROFILE);
        }

        @Override
        public String[] commandLineParameters() {
            return new String[] { "OuterProfile" };
        }

        @Override
        public boolean runMainMethod() {
            return true;
        }
    }

    public static class ModernEnglishProfile implements QuarkusTestProfile {

        @Override
        public String[] commandLineParameters() {
            return new String[] { "Hey" };
        }

        @Override
        public boolean runMainMethod() {
            return true;
        }
    }
}
