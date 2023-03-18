package io.quarkus.opentelemetry.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@Disabled("We need to move the AWS dependency testing to an independent module")
public class OpenTelemetryPropagatorsTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.opentelemetry.propagators", "tracecontext,xray")
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException {
        TextMapPropagator[] textMapPropagators = TestUtil.getTextMapPropagators(openTelemetry);

        //        assertThat(textMapPropagators, arrayContainingInAnyOrder(W3CTraceContextPropagator.getInstance(),
        //                AwsXrayPropagator.getInstance()));
    }
}
