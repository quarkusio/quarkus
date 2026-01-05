package io.quarkus.jfr.deployment;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jfr.api.IdProducer;
import io.quarkus.test.QuarkusUnitTest;

@ActivateRequestContext
public class JfrIdTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Inject
    IdProducer idProducer;

    @Test
    public void test() {
        String traceId = idProducer.getTraceId();
        String spanId = idProducer.getSpanId();

        Assertions.assertNotNull(traceId);
        Assertions.assertNull(spanId);
    }
}
