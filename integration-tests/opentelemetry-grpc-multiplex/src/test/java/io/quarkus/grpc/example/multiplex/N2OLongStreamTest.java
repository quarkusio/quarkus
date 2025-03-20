package io.quarkus.grpc.example.multiplex;

import io.opentelemetry.proto.trace.v1.Span;
import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.collector.OtelCollectorLifecycleManager;

import java.util.List;

@QuarkusTest
@TestProfile(N2OGRPCTestProfile.class)
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class N2OLongStreamTest extends BaseTest {
    @Override
    void verifySpans(List<Span> scopeSpans) {

    }
}
