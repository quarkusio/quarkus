package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class InvalidAttributesConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.otel.resource.attributes", "pod.id=${SOMETHING}")
            .assertException(new Consumer<Throwable>() {
                @Override
                public void accept(final Throwable throwable) {
                    throwable.getCause().printStackTrace();
                    assertTrue(throwable.getCause() instanceof NoSuchElementException);
                    assertEquals("SRCFG00011: Could not expand value SOMETHING in property quarkus.otel.resource.attributes",
                            throwable.getCause().getMessage());
                }
            });

    @Test
    void test() {
    }
}
