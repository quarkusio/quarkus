package io.quarkus.it.opentelemetry.vertx.grpc.exporter;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class NoTLSNoCompressionTest extends AbstractExporterTest {

}
