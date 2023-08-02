package io.quarkus.it.opentelemetry.vertx.exporter.grpc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.it.opentelemetry.vertx.exporter.OtelCollectorLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(GrpcWithTLSWithTrustAllWithCompressionTest.Profile.class)
public class GrpcWithTLSWithTrustAllWithCompressionTest extends AbstractExporterTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.tls.trust-all", "true");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return Collections.singletonList(
                    new TestResourceEntry(
                            OtelCollectorLifecycleManager.class,
                            Map.of("enableTLS", "true", "enableCompression", "true", "preventTrustCert", "true")));
        }
    }

}
