package io.quarkus.it.opentelemetry.vertx.exporter.grpc;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.collector.OtelCollectorLifecycleManager;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class GrpcNoTLSNoCompressionTest extends AbstractExporterTest {

}
