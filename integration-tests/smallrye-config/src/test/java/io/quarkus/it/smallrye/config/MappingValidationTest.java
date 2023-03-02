package io.quarkus.it.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.config.ConfigValidationException;

public class MappingValidationTest {
    @Test
    void validation() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(ValidationTest.class))
                .build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);

        List<Event> failingEvents = results.testEvents().failed().list();
        assertEquals(1, failingEvents.size());

        Throwable exception = failingEvents.get(0).getPayload(TestExecutionResult.class).get().getThrowable().get();
        Throwable cause = exception.getCause();
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertNotNull(cause);
        assertEquals(ConfigValidationException.class.getName(), cause.getClass().getName());
    }

    @QuarkusTest
    @TestProfile(ValidationTestProfile.class)
    public static class ValidationTest {
        @Inject
        Cloud cloud;

        @Test
        void fail() {
            Assertions.fail();
        }
    }

    public static class ValidationTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.test-port", "9999");
        }
    }
}
