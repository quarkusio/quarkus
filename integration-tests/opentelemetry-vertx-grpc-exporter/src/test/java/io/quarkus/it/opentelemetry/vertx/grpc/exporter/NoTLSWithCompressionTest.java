package io.quarkus.it.opentelemetry.vertx.grpc.exporter;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, initArgs = @ResourceArg(name = "enableCompression", value = "true"), restrictToAnnotatedClass = true)
public class NoTLSWithCompressionTest extends AbstractExporterTest {

}
