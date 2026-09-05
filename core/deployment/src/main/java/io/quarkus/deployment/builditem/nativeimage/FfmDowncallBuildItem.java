package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;

/**
 * Used to register a downcall signature for FFI/FFM runtime access.
 * <p>
 * Downcalls can optionally specify linker options that affect how GraalVM
 * generates the native call stub:
 * <ul>
 * <li>{@code captureCallState} — captures native error state (e.g. {@code errno})
 * after the call</li>
 * <li>{@code firstVariadicArg} — index of the first variadic argument for
 * variadic C functions (e.g. {@code printf}, {@code ioctl})</li>
 * <li>{@code critical} — marks the call as critical (no safepoint, no GC),
 * optionally allowing heap access</li>
 * </ul>
 * <p>
 * Use the builder to create downcall registrations:
 *
 * <pre>
 * // Simple downcall without options
 * FfmDowncallBuildItem.builder(FfmType.INT, FfmType.INT).build()
 *
 * // Downcall with options
 * FfmDowncallBuildItem.builder(FfmType.INT, FfmType.INT, FfmType.ADDRESS)
 *         .captureCallState()
 *         .firstVariadicArg(2)
 *         .build()
 * </pre>
 *
 * @see <a href=
 *      "https://www.graalvm.org/jdk25.2/reference-manual/native-image/native-code-interoperability/ffm-api/#linker-options">GraalVM
 *      FFM Linker Options</a>
 */
public final class FfmDowncallBuildItem extends FfmCallBuildItem {

    private final boolean captureCallState;
    private final int firstVariadicArg;
    private final CriticalOption critical;

    /**
     * Creates a downcall registration without any linker options.
     *
     * @param returnType the return type
     * @param parameterTypes the parameter types
     * @deprecated Use {@link #builder(FfmType, FfmType...)} instead
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public FfmDowncallBuildItem(FfmType returnType, FfmType... parameterTypes) {
        this(false, -1, null, returnType, parameterTypes);
    }

    private FfmDowncallBuildItem(boolean captureCallState, int firstVariadicArg,
            CriticalOption critical, FfmType returnType, FfmType... parameterTypes) {
        super(returnType, parameterTypes);
        this.captureCallState = captureCallState;
        this.firstVariadicArg = firstVariadicArg;
        this.critical = critical;
    }

    /**
     * Creates a builder for a downcall registration.
     *
     * @param returnType the return type
     * @param parameterTypes the parameter types
     * @return a new builder
     */
    public static Builder builder(FfmType returnType, FfmType... parameterTypes) {
        return new Builder(returnType, parameterTypes);
    }

    /**
     * Whether this downcall captures native error state (e.g. {@code errno}).
     */
    public boolean isCaptureCallState() {
        return captureCallState;
    }

    /**
     * The index of the first variadic argument, or {@code -1} if not set.
     */
    public int getFirstVariadicArg() {
        return firstVariadicArg;
    }

    /**
     * The critical option for this downcall, or {@code null} if not set.
     */
    public CriticalOption getCritical() {
        return critical;
    }

    /**
     * Whether any linker options are set on this downcall.
     */
    public boolean hasOptions() {
        return captureCallState || firstVariadicArg >= 0 || critical != null;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        FfmDowncallBuildItem that = (FfmDowncallBuildItem) o;
        return captureCallState == that.captureCallState
                && firstVariadicArg == that.firstVariadicArg
                && critical == that.critical;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), captureCallState, firstVariadicArg, critical);
    }

    /**
     * Specifies whether a critical downcall allows the native function to
     * access Java heap objects during execution.
     * <p>
     * Critical calls skip safepoints and GC, which improves performance
     * but restricts what the native function can do.
     *
     * @see <a href=
     *      "https://www.graalvm.org/jdk25.2/reference-manual/native-image/native-code-interoperability/ffm-api/#linker-options">GraalVM
     *      FFM Linker Options</a>
     */
    public enum CriticalOption {
        /** Critical call where the native function must NOT access Java heap objects. */
        NO_HEAP_ACCESS(false),
        /** Critical call where the native function may access Java heap objects. */
        ALLOW_HEAP_ACCESS(true);

        private final boolean allowHeapAccess;

        CriticalOption(boolean allowHeapAccess) {
            this.allowHeapAccess = allowHeapAccess;
        }

        public boolean allowHeapAccess() {
            return allowHeapAccess;
        }
    }

    public static class Builder {
        private final FfmType returnType;
        private final FfmType[] parameterTypes;
        private boolean captureCallState;
        private int firstVariadicArg = -1;
        private CriticalOption critical;

        private Builder(FfmType returnType, FfmType... parameterTypes) {
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        /**
         * Captures native error state (e.g. {@code errno}) after the downcall.
         * Corresponds to {@code Linker.Option.captureCallState("errno")}.
         */
        public Builder captureCallState() {
            this.captureCallState = true;
            return this;
        }

        /**
         * Sets the index of the first variadic argument for variadic C functions.
         * Corresponds to {@code Linker.Option.firstVariadicArg(index)}.
         *
         * @param index the zero-based index of the first variadic parameter
         */
        public Builder firstVariadicArg(int index) {
            this.firstVariadicArg = index;
            return this;
        }

        /**
         * Marks this downcall as critical (no safepoint, no GC during the call).
         * Corresponds to {@code Linker.Option.critical(allowHeapAccess)}.
         *
         * @param option whether the native function may access Java heap objects
         */
        public Builder critical(CriticalOption option) {
            this.critical = option;
            return this;
        }

        public FfmDowncallBuildItem build() {
            return new FfmDowncallBuildItem(captureCallState, firstVariadicArg,
                    critical, returnType, parameterTypes);
        }
    }
}
