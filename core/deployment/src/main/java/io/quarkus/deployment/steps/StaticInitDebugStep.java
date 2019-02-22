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

package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.graalvm.nativeimage.ImageInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;

/**
 * This build step introduces a small blurb into the top of every run-time initialized class that will throw an {@code Error}
 * with a useful stack trace if that class is somehow initialized during native image build time, as the GraalVM native image
 * tool does not give a usage point for such errors.
 */
public class StaticInitDebugStep {

    private static final BiFunction<String, ClassVisitor, ClassVisitor> TRANSFORMER = new BiFunction<String, ClassVisitor, ClassVisitor>() {
        @Override
        public ClassVisitor apply(final String className, final ClassVisitor classVisitor) {
            return new ClassVisitor(Opcodes.ASM6, classVisitor) {
                @Override
                public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                        final String signature, final String[] exceptions) {
                    final MethodVisitor outer = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (name.equals("<clinit>")) {
                        outer.visitMethodInsn(Opcodes.INVOKESTATIC, ImageInfo.class.getName(), "inImageBuildtimeCode", "()Z",
                                false);
                        Label ok = new Label();
                        outer.visitJumpInsn(Opcodes.IFEQ, ok);
                        // construct an error - todo this could go on some core runtime class to save a few bytes of repeated string
                        outer.visitTypeInsn(Opcodes.NEW, "java/lang/Error");
                        outer.visitLdcInsn("Class initialized during build");
                        outer.visitMethodInsn(Opcodes.INVOKESPECIAL, Error.class.getName(), "<init>", "(Ljava/lang/String;)V",
                                false);
                        outer.visitInsn(Opcodes.ATHROW);
                    }
                    return outer;
                }
            };
        }
    };

    @BuildStep
    public List<BytecodeTransformerBuildItem> addStaticInitDebug(List<RuntimeInitializedClassBuildItem> classes) {
        final ArrayList<BytecodeTransformerBuildItem> outputList = new ArrayList<>(classes.size());
        for (RuntimeInitializedClassBuildItem classBuildItem : classes) {
            outputList.add(new BytecodeTransformerBuildItem(classBuildItem.getClassName(), TRANSFORMER));
        }
        return outputList;
    }
}
