package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;

class QuarkusDeclarativeSdkTest {

    @SuppressWarnings("removal")
    @Test
    void everyNestedCustomizerIsBuiltIn() {
        // A built-in customizer missing from isBuiltIn would switch every application to the legacy engine
        List<String> nested = new ArrayList<>();
        for (Class<?> type : AutoConfiguredOpenTelemetrySdkBuilderCustomizer.class.getDeclaredClasses()) {
            if (AutoConfiguredOpenTelemetrySdkBuilderCustomizer.class.isAssignableFrom(type)
                    && !Modifier.isAbstract(type.getModifiers())) {
                nested.add(type.getName());
            }
        }

        assertThat(nested).hasSize(7).allMatch(QuarkusDeclarativeSdk::isBuiltIn);
        assertThat(QuarkusDeclarativeSdk.isBuiltIn("com.example.MyCustomizer")).isFalse();
    }
}
