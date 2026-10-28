package io.quarkus.aesh.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.nativeimage.FfmDowncallBuildItem;

/**
 * Guards {@link AeshNativeImageProcessor#registerFfmDowncalls} against silent
 * drift in aesh-readline's FFM downcalls.
 * <p>
 * A mismatched descriptor (e.g. a stale {@code captureCallState} after
 * upstream drops errno capturing) does not fail the build — the native
 * terminal silently falls back to ExecPty at runtime. This test reads the
 * actual downcall handle types from aesh-readline via reflection and asserts
 * the registered descriptors cover exactly them, so the next upstream
 * signature change fails here with the fix location attached.
 * <p>
 * FFM types are resolved reflectively ({@code Class.forName}) rather than
 * referenced directly because this module compiles against an older release
 * where {@code java.lang.foreign} is still preview.
 */
public class FfmDowncallDescriptorDriftTest {

    private static Class<?> memorySegmentType() {
        try {
            return Class.forName("java.lang.foreign.MemorySegment");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("java.lang.foreign.MemorySegment not available", e);
        }
    }

    @Test
    void registeredDescriptorsMatchNativeHandles() throws Exception {
        Assumptions.assumeFalse(isWindows(),
                "POSIX libc symbols do not resolve on Windows; covered by Linux/macOS CI");
        Assumptions.assumeTrue(Runtime.version().feature() >= 22,
                "FFM downcall handles live in multi-release versioned classes (Java 22+); nothing to check below that");

        Map<String, MethodType> actual = new HashMap<>();
        collectHandles("org.aesh.terminal.tty.impl.LibC", actual);
        collectHandles("org.aesh.terminal.detect.FfmPosix$Handles", actual);
        assertThat(actual).as("expected to find native handles via reflection").isNotEmpty();

        List<FfmDowncallBuildItem> registered = new ArrayList<>();
        new AeshNativeImageProcessor().registerFfmDowncalls(registered::add);
        Set<MethodType> registeredTypes = new HashSet<>();
        for (FfmDowncallBuildItem item : registered) {
            registeredTypes.add(toMethodType(item));
        }

        // Every real downcall must be registered, otherwise native image
        // fails with MissingForeignRegistrationError at runtime.
        for (Map.Entry<String, MethodType> entry : actual.entrySet()) {
            assertThat(registeredTypes)
                    .as("missing FFM registration for %s — update registerFfmDowncalls", entry.getKey())
                    .contains(entry.getValue());
        }

        // Every registration must correspond to a real downcall, with two
        // intentional extras: the off-platform poll() twin (macOS int vs
        // Linux long nfds_t) and the separate close entry sharing isatty's
        // descriptor. Compared as multisets, not sets, so that a redundant
        // duplicate entry — e.g. a stale capture variant colliding with an
        // existing type — fails instead of collapsing away.
        List<MethodType> expectedMulti = new ArrayList<>(new HashSet<>(actual.values()));
        expectedMulti.add(MethodType.methodType(int.class, int.class)); // close ≡ isatty, registered separately
        MethodType actualPoll = actual.get("LibC.POLL");
        assertThat(actualPoll).as("expected LibC.POLL handle").isNotNull();
        expectedMulti.add(pollTwin(actualPoll));
        List<MethodType> registeredMulti = new ArrayList<>();
        for (FfmDowncallBuildItem item : registered) {
            registeredMulti.add(toMethodType(item));
        }
        assertThat(registeredMulti).containsExactlyInAnyOrderElementsOf(expectedMulti);

        // ioctl keeps its variadic marker, without errno capture.
        FfmDowncallBuildItem ioctl = findByType(registered, actual.get("LibC.IOCTL"));
        assertThat(ioctl.isCaptureCallState()).isFalse();
        assertThat(ioctl.getFirstVariadicArg()).isEqualTo(2);

        // open (LibC) keeps its errno capture.
        FfmDowncallBuildItem open = findByType(registered, actual.get("LibC.OPEN"));
        assertThat(open.isCaptureCallState()).isTrue();
    }

    /**
     * Collects the types of all {@link MethodHandle} static fields of the
     * given holder class, keyed by simple class and field name.
     */
    private static void collectHandles(String className, Map<String, MethodType> out) throws Exception {
        Class<?> holder = Class.forName(className);
        for (Field field : holder.getDeclaredFields()) {
            if (field.getType() == MethodHandle.class && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                MethodHandle handle = (MethodHandle) field.get(null);
                if (handle != null) {
                    out.put(holder.getSimpleName() + "." + field.getName(), handle.type());
                }
            }
        }
    }

    /**
     * Converts a registered item to the {@link MethodType} of the downcall it
     * describes: an optional leading capture-state segment followed by the
     * mapped parameter types.
     */
    private static MethodType toMethodType(FfmDowncallBuildItem item) {
        List<Class<?>> parameters = new ArrayList<>();
        if (item.isCaptureCallState()) {
            parameters.add(memorySegmentType());
        }
        for (String type : item.getParameterTypes()) {
            parameters.add(toClass(type));
        }
        return MethodType.methodType(toClass(item.getReturnType()), parameters);
    }

    private static Class<?> toClass(String ffmType) {
        switch (ffmType) {
            case "void":
                return void.class;
            case "void*":
                return memorySegmentType();
            case "jint":
                return int.class;
            case "jlong":
                return long.class;
            default:
                throw new AssertionError("Unmapped FfmType '" + ffmType + "', extend the test");
        }
    }

    /**
     * Returns the cross-platform twin of a poll() descriptor (Linux
     * {@code long} vs macOS {@code int} nfds_t at parameter index 1).
     */
    private static MethodType pollTwin(MethodType poll) {
        Class<?>[] params = poll.parameterArray();
        assertThat(params)
                .as("poll() descriptor shape changed, revisit the twin logic")
                .hasSize(3);
        assertThat(params[0]).isEqualTo(memorySegmentType());
        assertThat(params[2]).isEqualTo(int.class);
        Class<?> twin = params[1] == long.class ? int.class : long.class;
        return MethodType.methodType(int.class, memorySegmentType(), twin, int.class);
    }

    private static FfmDowncallBuildItem findByType(List<FfmDowncallBuildItem> items, MethodType type) {
        for (FfmDowncallBuildItem item : items) {
            if (toMethodType(item).equals(type)) {
                return item;
            }
        }
        throw new AssertionError("No registered item for descriptor " + type);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }
}
