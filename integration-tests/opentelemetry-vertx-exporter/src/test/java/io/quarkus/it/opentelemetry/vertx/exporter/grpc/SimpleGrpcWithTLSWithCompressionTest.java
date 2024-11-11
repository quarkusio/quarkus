package io.quarkus.it.opentelemetry.vertx.exporter.grpc;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.it.opentelemetry.vertx.exporter.OtelCollectorLifecycleManager;
import io.quarkus.it.opentelemetry.vertx.exporter.SimpleProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, initArgs = {
        @ResourceArg(name = "enableTLS", value = "true"),
        @ResourceArg(name = "enableCompression", value = "true")
}, restrictToAnnotatedClass = true)
@TestProfile(SimpleProfile.class)
public class SimpleGrpcWithTLSWithCompressionTest extends AbstractExporterTest {

}
