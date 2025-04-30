package io.quarkus.observability.test;

import java.util.Map;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.devresource.lgtm.LgtmResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = LgtmResource.class, restrictToAnnotatedClass = true)
@TestProfile(LgtmLifecycleTest.TestResourceTestProfileOff.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmLifecycleTest extends LgtmConfigTestBase {

    public static class TestResourceTestProfileOff implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.micrometer.export.otlp.url", "http://${otel-collector.url}/v1/metrics",
                    "quarkus.otel.exporter.otlp.protocol", "http/protobuf",
                    "quarkus.otel.exporter.otlp.endpoint", "http://${otel-collector.url}",
                    "quarkus.observability.dev-resources", "false",
                    "quarkus.observability.enabled", "false");
        }
    }
}
