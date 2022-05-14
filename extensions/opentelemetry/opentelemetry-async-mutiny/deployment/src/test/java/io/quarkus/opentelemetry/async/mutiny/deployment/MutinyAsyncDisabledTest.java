package io.quarkus.opentelemetry.async.mutiny.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategies;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.TracingMulti;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.TracingUni;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MutinyAsyncDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.async.mutiny.enabled", "false");

    @Test
    void testAsyncStrategies() {
        assertThat(AsyncOperationEndStrategies.instance().resolveStrategy(Uni.class)).isNull();
        assertThat(AsyncOperationEndStrategies.instance().resolveStrategy(Multi.class)).isNull();

        // TODO: Not sure how much sense this makes. In the other test I was forced to register the interceptors
        //  manually. So when I do not do that here then this is true for sure.
        assertThat(Uni.createFrom().item("test")).isNotInstanceOf(TracingUni.class);
        assertThat(Multi.createFrom().item("test")).isNotInstanceOf(TracingMulti.class);
    }
}
