package io.quarkus.it.opentelemetry.vertx.exporter.http;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.it.opentelemetry.vertx.exporter.OtelCollectorLifecycleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, initArgs = {
        @ResourceArg(name = "enableTLS", value = "true"),
        @ResourceArg(name = "enableCompression", value = "true"),
        @ResourceArg(name = "protocol", value = "http/protobuf")
}, restrictToAnnotatedClass = true)
public class HttpWithTLSWithCompressionTest extends AbstractExporterTest {

}
