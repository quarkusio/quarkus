package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * Tests that QuarkusTestProfile works as expected
 */
@QuarkusTest
@TestProfile(GreetingProfileTestCase.MyProfile.class)
public class GreetingProfileTestCase {

    @Test
    public void included() {
        RestAssured.when()
                .get("/greeting/Stu")
                .then()
                .statusCode(200)
                .body(is("Hey Stu"));
    }

    @Test
    public void testPortTakesEffect() {
        Assertions.assertEquals(7777, RestAssured.port);
    }

    @Test
    public void testTestResourceState() {
        // 155 means that the TestResource was started but hasn't yet stopped
        Assertions.assertEquals(155, DummyTestResource.state.get());
    }

    public static class MyProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("quarkus.http.test-port", "7777");
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Collections.singleton(BonjourService.class);
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return Collections
                    .singletonList(new TestResourceEntry(DummyTestResource.class, Collections.singletonMap("num", "100")));
        }

        @Override
        public String[] commandLineParameters() {
            return new String[] { "Hey" };
        }

        @Override
        public boolean runMainMethod() {
            return true;
        }
    }

    /**
     * This only used to ensure that the TestResource has been handled correctly by the QuarkusTestExtension
     */
    public static class DummyTestResource implements QuarkusTestResourceLifecycleManager {

        public static final AtomicInteger state = new AtomicInteger(0);
        public static final int START_DELTA = 55;

        private Integer numArg;

        @Override
        public void init(Map<String, String> initArgs) {
            numArg = Integer.parseInt(initArgs.get("num"));
            state.set(numArg);
        }

        @Override
        public Map<String, String> start() {
            state.addAndGet(START_DELTA);
            return Collections.emptyMap();
        }

        @Override
        public void stop() {
            if (state.get() != (numArg + START_DELTA)) {
                throw new IllegalStateException("TestResource state was not properly handled");
            }
        }
    }
}
