package io.quarkus.gradle.application.internal.execution.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class QuarkusWorkerTest {

    private static final String FIRST = "quarkus.worker-reset.first";
    private static final String MUTATED = "quarkus.worker-reset.mutated";
    private static final String SECOND = "quarkus.worker-reset.second";
    private static final String PLATFORM = "platform.quarkus.worker-reset.platform";
    private static final String UNRELATED = "worker-reset.unrelated";
    private static final List<String> TEST_PROPERTIES = List.of(FIRST, MUTATED, SECOND, PLATFORM, UNRELATED);

    private final Project project = ProjectBuilder.builder().build();

    @Test
    void eachSubmissionReplacesOnlyQuarkusSystemPropertiesWithItsIntendedValues() {
        Map<String, String> originalValues = originalValues();
        try {
            System.setProperty(UNRELATED, "preserved");

            worker(Map.of(FIRST, "first-value", PLATFORM, "first-platform-value"))
                    .resetQuarkusSystemProperties();
            System.setProperty(MUTATED, "augmentation-mutation");

            worker(Map.of(SECOND, "second-value", PLATFORM, "second-platform-value", UNRELATED, "ignored"))
                    .resetQuarkusSystemProperties();

            assertThat(System.getProperties())
                    .doesNotContainKeys(FIRST, MUTATED)
                    .containsEntry(SECOND, "second-value")
                    .containsEntry(PLATFORM, "second-platform-value")
                    .containsEntry(UNRELATED, "preserved");
        } finally {
            restore(originalValues);
        }
    }

    private TestWorker worker(Map<String, String> intendedProperties) {
        MapProperty<String, String> forkedSystemProperties = project.getObjects().mapProperty(String.class, String.class);
        forkedSystemProperties.set(intendedProperties);
        QuarkusParams parameters = mock(QuarkusParams.class);
        when(parameters.getForkedSystemProperties()).thenReturn(forkedSystemProperties);
        return new TestWorker(parameters);
    }

    private static Map<String, String> originalValues() {
        Map<String, String> originalValues = new LinkedHashMap<>();
        for (String property : TEST_PROPERTIES) {
            originalValues.put(property, System.getProperty(property));
            System.clearProperty(property);
        }
        return originalValues;
    }

    private static void restore(Map<String, String> originalValues) {
        originalValues.forEach((property, value) -> {
            if (value == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, value);
            }
        });
    }

    private static final class TestWorker extends QuarkusWorker<QuarkusParams> {

        private final QuarkusParams parameters;

        private TestWorker(QuarkusParams parameters) {
            this.parameters = parameters;
        }

        @Override
        public QuarkusParams getParameters() {
            return parameters;
        }

        @Override
        public void execute() {
        }
    }
}
