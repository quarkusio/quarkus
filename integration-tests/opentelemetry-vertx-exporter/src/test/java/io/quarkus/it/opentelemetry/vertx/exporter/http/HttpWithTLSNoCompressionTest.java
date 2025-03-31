package io.quarkus.it.opentelemetry.vertx.exporter.http;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.collector.OtelCollectorLifecycleManager;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, initArgs = {
        @ResourceArg(name = "enableTLS", value = "true"),
        @ResourceArg(name = "protocol", value = "http/protobuf")
}, restrictToAnnotatedClass = true)
public class HttpWithTLSNoCompressionTest extends AbstractExporterTest {

}
