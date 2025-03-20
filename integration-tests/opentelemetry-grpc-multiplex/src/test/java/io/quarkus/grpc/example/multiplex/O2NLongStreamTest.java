package io.quarkus.grpc.example.multiplex;

import io.opentelemetry.proto.trace.v1.Span;
import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.collector.OtelCollectorLifecycleManager;

import java.util.List;

@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class O2NLongStreamTest extends BaseTest {
    @Override
    void verifySpans(List<Span> scopeSpans) {

    }
}
