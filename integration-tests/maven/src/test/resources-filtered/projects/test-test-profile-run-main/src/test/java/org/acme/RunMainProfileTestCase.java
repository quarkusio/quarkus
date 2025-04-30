package org.acme;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * Tests that QuarkusTestProfile works as expected
 */
@QuarkusTest
@TestProfile(RunMainProfileTestCase.MyProfile.class)
public class RunMainProfileTestCase {

    @Inject
    GreetingService greetingService;

    @Inject
    ExternalService externalService;

    @Test
    public void behaviourOverriddenOnCommandLine() {
        RestAssured.when()
                .get("/app/greeting/Sausage")
                .then()
                .statusCode(200)
                .body(is("Hey Sausage"));
    }

    @Test
    public void testPortTakesEffect() {
        assertEquals(7777, RestAssured.port);
    }

    @Test
    public void testTestResourceState() {
        // 155 means that the TestResource was started but hasn't yet stopped
        assertEquals(155, DummyTestResource.state.get());
    }

    @Test
    public void testContext() {
        assertEquals(MyProfile.class.getName(), DummyTestResource.testProfile.get());
    }

    @Test
    public void testProfileBeans() {
        assertEquals("Bonjour Foo", greetingService.greet("Foo"));
        assertEquals("profile", externalService.service());
        assertTrue(Arc.container().select(SharedNormalTestCase.class).isUnsatisfied());
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

        @Priority(1000) // Must be higher than priority of MockExternalService
        @Alternative
        @Produces
        public ExternalService externalService() {
            return new ExternalService() {
                @Override
                public String service() {
                    return "profile";
                }
            };
        }
    }

    /**
     * This only used to ensure that the TestResource has been handled correctly by the QuarkusTestExtension
     */
    public static class DummyTestResource implements QuarkusTestResourceLifecycleManager {

        public static final AtomicInteger state = new AtomicInteger(0);
        public static final AtomicReference<String> testProfile = new AtomicReference<>(null);
        public static final int START_DELTA = 55;

        private Integer numArg;

        @Override
        public void setContext(Context context) {
            testProfile.set(context.testProfile());
        }

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
