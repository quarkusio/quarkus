package io.quarkus.core.deployment.action.impl;

import io.smallrye.classfile.CodeBuilder;

/**
 * Emits bytecode that loads a captured value onto the operand stack.
 * <p>
 * Every captured value in a service action lambda resolves to a {@code CaptureEmitter}
 * during extraction. For simple constants this wraps a single {@code ldc} instruction;
 * for compound values (collections, maps) it may emit an entire instruction sequence
 * that reconstructs the value inline.
 */
@FunctionalInterface
public interface CaptureEmitter {

    /**
     * Emit bytecode that pushes this captured value onto the operand stack.
     *
     * @param code the code builder to emit into (never {@code null})
     */
    void emit(CodeBuilder code);
}
