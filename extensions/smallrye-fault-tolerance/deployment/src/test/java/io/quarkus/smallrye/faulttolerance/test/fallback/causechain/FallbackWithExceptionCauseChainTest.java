package io.quarkus.smallrye.faulttolerance.test.fallback.causechain;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FallbackWithExceptionCauseChainTest {
    @RegisterExtension
    final static QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ExpectedOutcomeException.class, FallbackWithApplyOn.class,
                    FallbackWithBothSkipOnAndApplyOn.class, FallbackWithSkipOn.class));

    @Inject
    FallbackWithBothSkipOnAndApplyOn fallbackWithBothSkipOnAndApplyOn;

    @Inject
    FallbackWithSkipOn fallbackWithSkipOn;

    @Inject
    FallbackWithApplyOn fallbackWithApplyOn;

    @Test
    public void bothSkipOnAndApplyOn() {
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new RuntimeException()))
                .isExactlyInstanceOf(RuntimeException.class);
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new RuntimeException(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new RuntimeException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(RuntimeException.class);

        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new Exception()))
                .isExactlyInstanceOf(Exception.class);
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new Exception(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new Exception(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(Exception.class);

        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new IOException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new IOException(new Exception())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new IOException(new ExpectedOutcomeException())))
                .doesNotThrowAnyException();

        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new ExpectedOutcomeException()))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new ExpectedOutcomeException(new Exception())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> fallbackWithBothSkipOnAndApplyOn.hello(new ExpectedOutcomeException(new IOException())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
    }

    @Test
    public void skipOn() {
        assertThatCode(() -> fallbackWithSkipOn.hello(new RuntimeException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithSkipOn.hello(new RuntimeException(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithSkipOn.hello(new RuntimeException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(RuntimeException.class);

        assertThatCode(() -> fallbackWithSkipOn.hello(new Exception()))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithSkipOn.hello(new Exception(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithSkipOn.hello(new Exception(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(Exception.class);

        assertThatCode(() -> fallbackWithSkipOn.hello(new IOException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithSkipOn.hello(new IOException(new Exception())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithSkipOn.hello(new IOException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(IOException.class);

        assertThatCode(() -> fallbackWithSkipOn.hello(new ExpectedOutcomeException()))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> fallbackWithSkipOn.hello(new ExpectedOutcomeException(new Exception())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> fallbackWithSkipOn.hello(new ExpectedOutcomeException(new IOException())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
    }

    @Test
    public void applyOn() {
        assertThatCode(() -> fallbackWithApplyOn.hello(new RuntimeException()))
                .isExactlyInstanceOf(RuntimeException.class);
        assertThatCode(() -> fallbackWithApplyOn.hello(new RuntimeException(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithApplyOn.hello(new RuntimeException(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(RuntimeException.class);

        assertThatCode(() -> fallbackWithApplyOn.hello(new Exception()))
                .isExactlyInstanceOf(Exception.class);
        assertThatCode(() -> fallbackWithApplyOn.hello(new Exception(new IOException())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithApplyOn.hello(new Exception(new ExpectedOutcomeException())))
                .isExactlyInstanceOf(Exception.class);

        assertThatCode(() -> fallbackWithApplyOn.hello(new IOException()))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithApplyOn.hello(new IOException(new Exception())))
                .doesNotThrowAnyException();
        assertThatCode(() -> fallbackWithApplyOn.hello(new IOException(new ExpectedOutcomeException())))
                .doesNotThrowAnyException();

        assertThatCode(() -> fallbackWithApplyOn.hello(new ExpectedOutcomeException()))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> fallbackWithApplyOn.hello(new ExpectedOutcomeException(new Exception())))
                .isExactlyInstanceOf(ExpectedOutcomeException.class);
        assertThatCode(() -> fallbackWithApplyOn.hello(new ExpectedOutcomeException(new IOException())))
                .doesNotThrowAnyException();
    }
}
