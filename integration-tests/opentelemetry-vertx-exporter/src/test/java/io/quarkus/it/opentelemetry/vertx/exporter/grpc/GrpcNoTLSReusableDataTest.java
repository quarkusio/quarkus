package io.quarkus.it.opentelemetry.vertx.exporter.grpc;

import io.quarkus.it.opentelemetry.vertx.exporter.AbstractExporterTest;
import io.quarkus.it.opentelemetry.vertx.exporter.OtelCollectorLifecycleManager;
import io.quarkus.it.opentelemetry.vertx.exporter.ReusableDataProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ReusableDataProfile.class)
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class GrpcNoTLSReusableDataTest extends AbstractExporterTest {

}
