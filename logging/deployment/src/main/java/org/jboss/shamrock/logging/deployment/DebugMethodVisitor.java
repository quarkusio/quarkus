/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.logging.deployment;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 */
public class DebugMethodVisitor extends MethodVisitor {
    public DebugMethodVisitor(final MethodVisitor methodVisitor) {
        super(Opcodes.ASM6, methodVisitor);
    }

    private int getCallerLine() {
        return new Throwable().getStackTrace()[2].getLineNumber();
    }

    public void visitInsn(final int opcode) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitInsn(opcode);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitIntInsn(final int opcode, final int operand) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitIntInsn(opcode, operand);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitVarInsn(final int opcode, final int var) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitVarInsn(opcode, var);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitTypeInsn(final int opcode, final String type) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitTypeInsn(opcode, type);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitFieldInsn(opcode, owner, name, descriptor);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitMethodInsn(opcode, owner, name, descriptor);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitJumpInsn(opcode, label);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitLdcInsn(final Object value) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitLdcInsn(value);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitIincInsn(final int var, final int increment) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitIincInsn(var, increment);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitTableSwitchInsn(min, max, dflt, labels);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitLookupSwitchInsn(dflt, keys, labels);
        super.visitLineNumber(getCallerLine(), pre);
    }

    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        final Label pre = new Label();
        super.visitLabel(pre);
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        super.visitLineNumber(getCallerLine(), pre);
    }
}
