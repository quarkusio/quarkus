package io.quarkus.smallrye.faulttolerance.test.retry.when;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RetryWhenResultAndExceptionTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RetryWhenResultAndExceptionService.class, IsNull.class, IsIllegalArgumentException.class));

    @Inject
    RetryWhenResultAndExceptionService service;

    @Test
    public void test() {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(service.getAttempts()).hasValue(3);
    }
}
