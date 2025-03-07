package io.quarkus.it.main.testing.repro42006;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.quarkus.test.junit.QuarkusTest;

// fails with `java.lang.ClassNotFoundException: io.quarkus.it.main.testing.repro42006.Repro42006Test$LambdaProvider$$Lambda$4007/0x000075d5017e8450`
@QuarkusTest
public class Repro42006Test {
    @ParameterizedTest
    @ArgumentsSource(LambdaProvider.class)
    void test(String type, Object lambda) {
        assertTrue(lambda.toString().contains("$$Lambda"), "Failed on " + type);
    }

    private static class LambdaProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("SerializableSupplier", (SerializableSupplier) () -> "foo"),
                    Arguments.of("SerializableCustom", (SerializableCustom) () -> "bar"));
        }
    }

    public interface SerializableSupplier extends Supplier<String>, Serializable {
    }

    public interface SerializableCustom extends Serializable {
        String get();
    }
}
