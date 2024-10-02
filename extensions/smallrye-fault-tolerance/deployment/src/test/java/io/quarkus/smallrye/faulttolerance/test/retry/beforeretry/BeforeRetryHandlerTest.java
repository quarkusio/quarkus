package io.quarkus.smallrye.faulttolerance.test.retry.beforeretry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BeforeRetryHandlerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeforeRetryHandlerService.class, MyDependency.class));

    @Inject
    BeforeRetryHandlerService service;

    @Test
    public void test() {
        assertThrows(IllegalArgumentException.class, service::hello);
        assertThat(BeforeRetryHandlerService.ids)
                .hasSize(3)
                .containsExactly(1, 2, 3);
    }
}
