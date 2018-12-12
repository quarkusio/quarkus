/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.protean.gizmo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MethodCreatorImpl extends BytecodeCreatorImpl implements MethodCreator {

    private int modifiers = Opcodes.ACC_PUBLIC;
    private final List<String> exceptions = new ArrayList<>();
    private final List<AnnotationCreatorImpl> annotations = new ArrayList<>();
    private final Map<Integer, AnnotationParameters> parameterAnnotations = new HashMap<>();

    private final MethodDescriptor methodDescriptor;
    private final String declaringClassName;
    private final ClassOutput classOutput;
    private final ClassCreator classCreator;

    MethodCreatorImpl(BytecodeCreatorImpl enclosing, MethodDescriptor methodDescriptor, String declaringClassName, ClassOutput classOutput, ClassCreator classCreator) {
        super(enclosing, true);
        this.methodDescriptor = methodDescriptor;
        this.declaringClassName = declaringClassName;
        this.classOutput = classOutput;
        this.classCreator = classCreator;
    }

    @Override
    public MethodCreator addException(String exception) {
        exceptions.add(exception.replace('.', '/'));
        return this;
    }

    @Override
    public List<String> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    @Override
    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    @Override
    public AnnotatedElement getParameterAnnotations(int param) {
        if(parameterAnnotations.containsKey(param)) {
            return parameterAnnotations.get(param);
        }
        AnnotationParameters p = new AnnotationParameters();
        parameterAnnotations.put(param, p);
        return p;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public MethodCreator setModifiers(int mods) {
        this.modifiers = mods;
        return this;
    }

    @Override
    public void write(ClassWriter file) {
        MethodVisitor visitor = file.visitMethod(modifiers, methodDescriptor.getName(), methodDescriptor.getDescriptor(), null, exceptions.toArray(new String[0]));



        int localVarCount = Modifier.isStatic(modifiers) ? 0 : 1;
        for (int i = 0; i < methodDescriptor.getParameterTypes().length; ++i) {
            String s = methodDescriptor.getParameterTypes()[i];
            if (s.equals("J") || s.equals("D")) {
                localVarCount += 2;
            } else {
                localVarCount++;
            }
        }
        int varCount = allocateLocalVariables(localVarCount);
        writeOperations(visitor);
        visitor.visitMaxs(0, varCount);

        for(AnnotationCreatorImpl annotation : annotations) {
            AnnotationVisitor av = visitor.visitAnnotation(DescriptorUtils.extToInt(annotation.getAnnotationType()), true);
            for(Map.Entry<String, Object> e : annotation.getValues().entrySet()) {
                av.visit(e.getKey(), e.getValue());
            }
            av.visitEnd();
        }
        for(Map.Entry<Integer, AnnotationParameters> entry : parameterAnnotations.entrySet()) {
            for(AnnotationCreatorImpl annotation : entry.getValue().annotations) {
                AnnotationVisitor av = visitor.visitParameterAnnotation(entry.getKey(), DescriptorUtils.extToInt(annotation.getAnnotationType()), true);
                for(Map.Entry<String, Object> e : annotation.getValues().entrySet()) {
                    av.visit(e.getKey(), e.getValue());
                }
                av.visitEnd();
            }
        }
        visitor.visitEnd();
    }

    @Override
    ResultHandle resolve(final ResultHandle handle) {
        return handle;
    }

    @Override
    ResultHandle[] resolve(final ResultHandle... handles) {
        return handles;
    }

    @Override
    public String toString() {
        return "MethodCreatorImpl [declaringClassName=" + getDeclaringClassName() + ", methodDescriptor=" + methodDescriptor + "]";
    }

    @Override
    public AnnotationCreator addAnnotation(String annotationType) {
        AnnotationCreatorImpl ac = new AnnotationCreatorImpl(annotationType);
        annotations.add(ac);
        return ac;
    }

    String getDeclaringClassName() {
        return declaringClassName;
    }

    ClassOutput getClassOutput() {
        return classOutput;
    }

    ClassCreator getClassCreator() {
        return classCreator;
    }

    FunctionCreatorImpl addFunctionBody(final ResultHandle instance, final ClassCreator cc, final MethodCreatorImpl mc, final BytecodeCreatorImpl owner) {
        FunctionCreatorImpl fc = new FunctionCreatorImpl(instance, cc, mc, owner);
        operations.add(new Operation() {
            @Override
            void writeBytecode(final MethodVisitor methodVisitor) {
                fc.getBytecode().writeOperations(methodVisitor);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.emptySet();
            }

            @Override
            ResultHandle getTopResultHandle() {
                return null;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }

            @Override
            public void findResultHandles(final Set<ResultHandle> vc) {
                fc.getBytecode().findActiveResultHandles(vc);
            }
        });
        return fc;
    }

    private static class AnnotationParameters implements AnnotatedElement {

        final List<AnnotationCreatorImpl> annotations = new ArrayList<>();

        @Override
        public AnnotationCreator addAnnotation(String annotationType) {
            AnnotationCreatorImpl ret = new AnnotationCreatorImpl(annotationType);
            annotations.add(ret);
            return ret;
        }
    }

}
