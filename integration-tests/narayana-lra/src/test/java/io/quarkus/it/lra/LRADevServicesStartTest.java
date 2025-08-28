package io.quarkus.it.lra;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LRADevServicesStartTest {

    DevServicesContext context;

    @Test
    public void testDevServicesStarted() {
        assertThat(context.devServicesProperties(), hasKey("quarkus.lra.coordinator-url"));
        assertThat(context.devServicesProperties(), hasKey("quarkus.lra.base-uri"));
        assertThat(context.devServicesProperties(), hasKey("quarkus.http.host"));
    }
}
