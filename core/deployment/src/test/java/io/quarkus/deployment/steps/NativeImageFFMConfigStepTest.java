package io.quarkus.deployment.steps;

import static io.quarkus.deployment.builditem.nativeimage.FfmType.ADDRESS;
import static io.quarkus.deployment.builditem.nativeimage.FfmType.INT;
import static io.quarkus.deployment.builditem.nativeimage.FfmType.LONG;
import static io.quarkus.deployment.builditem.nativeimage.FfmType.VOID;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.FfmDowncallBuildItem;
import io.quarkus.deployment.builditem.nativeimage.FfmDowncallBuildItem.CriticalOption;
import io.quarkus.deployment.builditem.nativeimage.FfmUpcallBuildItem;

class NativeImageFFMConfigStepTest {

    @Test
    void simpleDowncallWithoutOptions() {
        String json = generateJson(List.of(FfmDowncallBuildItem.builder(INT, INT).build()), List.of());

        assertThat(json).contains("\"returnType\":\"jint\"");
        assertThat(json).contains("\"parameterTypes\":[\"jint\"]");
        assertThat(json).doesNotContain("options");
    }

    @Test
    void downcallWithCaptureCallState() {
        String json = generateJson(
                List.of(FfmDowncallBuildItem.builder(INT, ADDRESS, INT)
                        .captureCallState()
                        .build()),
                List.of());

        assertThat(json).contains("\"returnType\":\"jint\"");
        assertThat(json).contains("\"parameterTypes\":[\"void*\",\"jint\"]");
        assertThat(json).contains("\"captureCallState\":true");
    }

    @Test
    void downcallWithFirstVariadicArg() {
        String json = generateJson(
                List.of(FfmDowncallBuildItem.builder(INT, INT, LONG, ADDRESS)
                        .firstVariadicArg(2)
                        .build()),
                List.of());

        assertThat(json).contains("\"firstVariadicArg\":2");
        assertThat(json).doesNotContain("captureCallState");
    }

    @Test
    void downcallWithCaptureCallStateAndFirstVariadicArg() {
        String json = generateJson(
                List.of(FfmDowncallBuildItem.builder(INT, INT, LONG, ADDRESS)
                        .captureCallState()
                        .firstVariadicArg(2)
                        .build()),
                List.of());

        assertThat(json).contains("\"captureCallState\":true");
        assertThat(json).contains("\"firstVariadicArg\":2");
    }

    @Test
    void downcallWithCriticalNoHeapAccess() {
        String json = generateJson(
                List.of(FfmDowncallBuildItem.builder(VOID, ADDRESS)
                        .critical(CriticalOption.NO_HEAP_ACCESS)
                        .build()),
                List.of());

        assertThat(json).contains("\"critical\":{\"allowHeapAccess\":false}");
    }

    @Test
    void downcallWithCriticalAllowHeapAccess() {
        String json = generateJson(
                List.of(FfmDowncallBuildItem.builder(VOID, ADDRESS)
                        .critical(CriticalOption.ALLOW_HEAP_ACCESS)
                        .build()),
                List.of());

        assertThat(json).contains("\"critical\":{\"allowHeapAccess\":true}");
    }

    @Test
    void mixOfDowncallsWithAndWithoutOptions() {
        String json = generateJson(
                List.of(
                        FfmDowncallBuildItem.builder(INT, INT).build(),
                        FfmDowncallBuildItem.builder(INT, INT)
                                .captureCallState()
                                .build()),
                List.of());

        // Should have two downcall entries
        assertThat(json).contains("\"downcalls\":[");
        // One without options, one with
        assertThat(json).contains("\"captureCallState\":true");
    }

    @Test
    void duplicateDowncallsAreDeduplicatedBySignatureAndOptions() {
        String json = generateJson(
                List.of(
                        FfmDowncallBuildItem.builder(INT, INT).build(),
                        FfmDowncallBuildItem.builder(INT, INT).build()),
                List.of());

        // Count occurrences of returnType — should appear once (deduplicated)
        long count = json.chars().filter(c -> c == '{').count();
        // root + foreign + one downcall = 3
        assertThat(count).isEqualTo(3);
    }

    @Test
    void sameSignatureDifferentOptionsAreNotDeduplicated() {
        String json = generateJson(
                List.of(
                        FfmDowncallBuildItem.builder(INT, INT).build(),
                        FfmDowncallBuildItem.builder(INT, INT)
                                .captureCallState()
                                .build()),
                List.of());

        // Both should be present — different options means different registrations
        // root + foreign + two downcalls + options = 5 objects
        long count = json.chars().filter(c -> c == '{').count();
        assertThat(count).isEqualTo(5);
    }

    @Test
    void simpleUpcall() {
        String json = generateJson(List.of(), List.of(new FfmUpcallBuildItem(INT, ADDRESS, ADDRESS)));

        assertThat(json).contains("\"upcalls\":[");
        assertThat(json).contains("\"returnType\":\"jint\"");
        assertThat(json).contains("\"parameterTypes\":[\"void*\",\"void*\"]");
        assertThat(json).doesNotContain("downcalls");
    }

    @Test
    void emptyListsProduceNoOutput() {
        NativeImageFFMConfigStep step = new NativeImageFFMConfigStep();
        List<GeneratedResourceBuildItem> produced = new ArrayList<>();

        step.generateFfmConfig(produced::add, List.of(), List.of());

        assertThat(produced).isEmpty();
    }

    private static String generateJson(List<FfmDowncallBuildItem> downcalls, List<FfmUpcallBuildItem> upcalls) {
        NativeImageFFMConfigStep step = new NativeImageFFMConfigStep();
        List<GeneratedResourceBuildItem> produced = new ArrayList<>();

        step.generateFfmConfig(produced::add, downcalls, upcalls);

        assertThat(produced).hasSize(1);
        assertThat(produced.get(0).getName())
                .isEqualTo("META-INF/native-image/foreign/reachability-metadata.json");
        return new String(produced.get(0).getData(), StandardCharsets.UTF_8);
    }
}
