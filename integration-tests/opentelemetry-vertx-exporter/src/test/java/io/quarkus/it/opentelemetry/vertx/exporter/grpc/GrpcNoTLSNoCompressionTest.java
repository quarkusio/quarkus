package io.quarkus.it.opentelemetry.vertx.exporter.grpc;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.it.opentelemetry.vertx.exporter.OtelCollectorLifecycleManager;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(OtelCollectorLifecycleManager.class)
public class GrpcNoTLSNoCompressionTest extends AbstractExporterTest {

}
