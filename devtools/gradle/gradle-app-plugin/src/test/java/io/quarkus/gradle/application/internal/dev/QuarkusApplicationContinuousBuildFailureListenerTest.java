package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFailureResult;
import org.gradle.tooling.events.task.TaskFinishEvent;
import org.gradle.tooling.events.task.TaskOperationDescriptor;
import org.gradle.tooling.events.task.TaskSkippedResult;
import org.gradle.tooling.events.task.TaskSuccessResult;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.dev.BuildOutputFailureKind;

class QuarkusApplicationContinuousBuildFailureListenerTest {

    @Test
    void reportsConfiguredMainAndTestFailures() {
        var listener = listener(Map.of(
                ":compileJava", BuildOutputFailureKind.MAIN,
                ":compileTestJava", BuildOutputFailureKind.TEST));

        listener.onFinish(taskEvent(":compileJava", mock(TaskFailureResult.class)));
        listener.onFinish(taskEvent(":compileTestJava", mock(TaskFailureResult.class)));

        assertThat(listener.awaitFailure(":compileJava")).isEqualTo(BuildOutputFailureKind.MAIN);
        assertThat(listener.awaitFailure(":compileTestJava")).isEqualTo(BuildOutputFailureKind.TEST);
    }

    @Test
    void treatsSuccessfulConfiguredTaskAsNotFailedAndIgnoresUnrelatedTask() {
        var listener = listener(Map.of(":compileJava", BuildOutputFailureKind.MAIN));

        listener.onFinish(taskEvent(":compileJava", mock(TaskSuccessResult.class)));
        listener.onFinish(taskEvent(":other:compileJava", mock(TaskFailureResult.class)));

        assertThat(listener.awaitFailure(":compileJava")).isNull();
    }

    @Test
    void treatsSkippedConfiguredTaskAsNotFailed() {
        var listener = listener(Map.of(":app:compileJava", BuildOutputFailureKind.MAIN));

        listener.onFinish(taskEvent(":app:compileJava", mock(TaskSkippedResult.class)));

        assertThat(listener.awaitFailure(":app:compileJava")).isNull();
    }

    @Test
    void awaitsAsynchronouslyDeliveredTaskEvent() throws Exception {
        var listener = listener(Map.of(":compileJava", BuildOutputFailureKind.MAIN));

        CompletableFuture<BuildOutputFailureKind> awaited = CompletableFuture.supplyAsync(
                () -> listener.awaitFailure(":compileJava"));
        listener.onFinish(taskEvent(":compileJava", mock(TaskFailureResult.class)));

        assertThat(awaited.get(5, TimeUnit.SECONDS)).isEqualTo(BuildOutputFailureKind.MAIN);
    }

    @Test
    void failureStateChannelBytecodeDoesNotReferenceMutableProjectOrTaskState() throws IOException {
        assertThat(classBytecode(QuarkusApplicationContinuousBuildFailureListener.class))
                .contains(internalName(BuildService.class), internalName(OperationCompletionListener.class))
                .doesNotContain(
                        internalName(Project.class),
                        internalName(Task.class),
                        internalName(TaskContainer.class));
        assertThat(classBytecode(QuarkusApplicationContinuousBuildFailureTask.class))
                .doesNotContain(
                        internalName(Project.class),
                        internalName(Task.class),
                        internalName(TaskContainer.class));
    }

    private static QuarkusApplicationContinuousBuildFailureListener listener(
            Map<String, BuildOutputFailureKind> failureKinds) {
        var parameters = mock(QuarkusApplicationContinuousBuildFailureListener.Parameters.class);
        @SuppressWarnings("unchecked")
        MapProperty<String, BuildOutputFailureKind> configuredFailureKinds = mock(MapProperty.class);
        when(parameters.getFailureKinds()).thenReturn(configuredFailureKinds);
        when(configuredFailureKinds.get()).thenReturn(failureKinds);
        return new TestListener(parameters);
    }

    private static TaskFinishEvent taskEvent(String taskPath,
            org.gradle.tooling.events.task.TaskOperationResult result) {
        TaskOperationDescriptor descriptor = mock(TaskOperationDescriptor.class);
        when(descriptor.getTaskPath()).thenReturn(taskPath);
        TaskFinishEvent event = mock(TaskFinishEvent.class);
        when(event.getDescriptor()).thenReturn(descriptor);
        when(event.getResult()).thenReturn(result);
        return event;
    }

    private static String internalName(Class<?> type) {
        return type.getName().replace('.', '/');
    }

    private static String classBytecode(Class<?> type) throws IOException {
        String classResource = type.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (var input = type.getClassLoader().getResourceAsStream(classResource)) {
            assertThat(input).as(classResource).isNotNull();
            classBytes = input.readAllBytes();
        }
        return new String(classBytes, StandardCharsets.ISO_8859_1);
    }

    private static final class TestListener extends QuarkusApplicationContinuousBuildFailureListener {

        private final Parameters parameters;

        private TestListener(Parameters parameters) {
            this.parameters = parameters;
        }

        @Override
        public Parameters getParameters() {
            return parameters;
        }
    }
}
