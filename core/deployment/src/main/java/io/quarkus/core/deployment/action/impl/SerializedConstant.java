package io.quarkus.core.deployment.action.impl;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;

import io.smallrye.serial.Serialized;

/**
 * A {@link Serialized} representation of a constant value ({@link ConstantDesc}).
 * <p>
 * Produced by {@link ConstantDescSerializer} for objects that are {@link ConstantDesc}
 * or {@link java.lang.constant.Constable} and resolve to a {@code ConstantDesc}.
 * The emitter loads this as a JVM constant via {@code ldc} or equivalent,
 * with boxing decided by the target context.
 * <p>
 * For {@link Boolean}, {@link Byte}, {@link Short}, and {@link Character} values,
 * the constant is stored as its {@code int} equivalent (suitable for primitive slots),
 * and the {@link #boxType()} carries the original wrapper type for reference-target boxing.
 */
public final class SerializedConstant extends Serialized {
    private final ConstantDesc constantDesc;
    private final ClassDesc boxType;

    /**
     * Construct a new instance with no box type (the constant is self-describing).
     *
     * @param constantDesc the constant descriptor (must not be {@code null})
     */
    public SerializedConstant(ConstantDesc constantDesc) {
        this(constantDesc, null);
    }

    /**
     * Construct a new instance with an explicit box type.
     *
     * @param constantDesc the constant descriptor (must not be {@code null})
     * @param boxType the original wrapper type for boxing, or {@code null} if not applicable
     */
    public SerializedConstant(ConstantDesc constantDesc, ClassDesc boxType) {
        this.constantDesc = constantDesc;
        this.boxType = boxType;
    }

    /**
     * {@return the constant descriptor}
     */
    public ConstantDesc constantDesc() {
        return constantDesc;
    }

    /**
     * {@return the original wrapper type for boxing, or {@code null} if the constant
     * is self-describing (Integer, Long, Float, Double, String, etc.)}
     */
    public ClassDesc boxType() {
        return boxType;
    }
}
