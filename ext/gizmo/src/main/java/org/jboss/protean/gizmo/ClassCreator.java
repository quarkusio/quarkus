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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassCreator implements AutoCloseable, AnnotatedElement {

    public static Builder builder() {
        return new Builder();
    }

    private final BytecodeCreatorImpl enclosing;
    private final ClassOutput classOutput;
    private final String superClass;
    private final String[] interfaces;
    private final Map<MethodDescriptor, MethodCreatorImpl> methods = new HashMap<>();
    private final Map<FieldDescriptor, FieldCreatorImpl> fields = new HashMap<>();
    private final List<AnnotationCreatorImpl> annotations = new ArrayList<>();
    private final String className;
    private final String signature;
    private final Map<MethodDescriptor, MethodDescriptor> superclassAccessors = new HashMap<>();
    private static final AtomicInteger accessorCount = new AtomicInteger();

    ClassCreator(BytecodeCreatorImpl enclosing, ClassOutput classOutput, String name, String signature, String superClass, String... interfaces) {
        this.enclosing = enclosing;
        this.classOutput = classOutput;
        this.superClass = superClass.replace('.', '/');
        this.interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            this.interfaces[i] = interfaces[i].replace('.', '/');
        }
        this.className = name.replace('.', '/');
        this.signature = signature;
    }

    public ClassCreator(ClassOutput classOutput, String name, String signature, String superClass, String... interfaces) {
        this(null, classOutput, name, signature, superClass, interfaces);
    }

    public MethodCreator getMethodCreator(MethodDescriptor methodDescriptor) {
        if (methods.containsKey(methodDescriptor)) {
            return methods.get(methodDescriptor);
        }
        MethodCreatorImpl creator = new MethodCreatorImpl(enclosing, methodDescriptor, className, classOutput, this);
        methods.put(methodDescriptor, creator);
        return creator;
    }

    public MethodCreator getMethodCreator(String name, String returnType, String... parameters) {
        return getMethodCreator(MethodDescriptor.ofMethod(className, name, returnType, parameters));
    }

    public MethodCreator getMethodCreator(String name, Class<?> returnType, Class<?>... parameters) {
        String[] params = new String[parameters.length];
        for (int i = 0; i < parameters.length; ++i) {
            params[i] = DescriptorUtils.classToStringRepresentation(parameters[i]);
        }
        return getMethodCreator(name, DescriptorUtils.classToStringRepresentation(returnType), params);
    }

    public MethodCreator getMethodCreator(String name, Object returnType, Object... parameters) {
        return getMethodCreator(MethodDescriptor.ofMethod(className, name, returnType, parameters));
    }

    public FieldCreator getFieldCreator(String name, String type) {
        return getFieldCreator(FieldDescriptor.of(className, name, type));
    }

    public FieldCreator getFieldCreator(String name, Object type) {
        return getFieldCreator(FieldDescriptor.of(className, name, DescriptorUtils.objectToDescriptor(type)));
    }

    public FieldCreator getFieldCreator(FieldDescriptor fieldDescriptor) {
        FieldCreatorImpl field = fields.get(fieldDescriptor);
        if (field == null) {
            field = new FieldCreatorImpl(fieldDescriptor);
            fields.put(fieldDescriptor, field);
        }
        return field;
    }

    public String getSuperClass() {
        return superClass;
    }

    public String getClassName() {
        return className;
    }

    MethodDescriptor getSuperclassAccessor(MethodDescriptor descriptor) {
        if (superclassAccessors.containsKey(descriptor)) {
            return superclassAccessors.get(descriptor);
        }
        String name = descriptor.getName() + "$$superaccessor" + accessorCount.incrementAndGet();
        MethodCreator ctor = getMethodCreator(name, descriptor.getReturnType(), descriptor.getParameterTypes());
        ResultHandle[] params = new ResultHandle[descriptor.getParameterTypes().length];
        for (int i = 0; i < params.length; ++i) {
            params[i] = ctor.getMethodParam(i);
        }
        ResultHandle ret = ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(getSuperClass(), descriptor.getName(), descriptor.getReturnType(), descriptor.getParameterTypes()), ctor.getThis(), params);
        ctor.returnValue(ret);
        superclassAccessors.put(descriptor, ctor.getMethodDescriptor());
        return ctor.getMethodDescriptor();
    }

    public void close() {
        ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String[] interfaces = new String[this.interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = this.interfaces[i];
        }
        file.visit(Opcodes.V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, className, signature, superClass, interfaces);

        boolean requiresCtor = true;
        for (MethodDescriptor m : methods.keySet()) {
            if (m.getName().equals("<init>")) {
                requiresCtor = false;
                break;
            }
        }

        if (requiresCtor) {
            // constructor
            MethodVisitor mv = file.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0); // push `this` to the operand stack
            mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V", false); // call the constructor of super class
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }

        //now add the fields
        for (Map.Entry<FieldDescriptor, FieldCreatorImpl> field : fields.entrySet()) {
            field.getValue().write(file);
        }

        for (Map.Entry<MethodDescriptor, MethodCreatorImpl> method : methods.entrySet()) {
            method.getValue().write(file);
        }
        for(AnnotationCreatorImpl annotation : annotations) {
            AnnotationVisitor av = file.visitAnnotation(DescriptorUtils.extToInt(annotation.getAnnotationType()), true);
            for(Map.Entry<String, Object> e : annotation.getValues().entrySet()) {
                av.visit(e.getKey(), e.getValue());
            }
            av.visitEnd();
        }

        file.visitEnd();

        classOutput.write(className, file.toByteArray());
    }

    @Override
    public AnnotationCreator addAnnotation(String annotationType) {
        AnnotationCreatorImpl ac = new AnnotationCreatorImpl(annotationType);
        annotations.add(ac);
        return ac;
    }

    public static class Builder {

        private ClassOutput classOutput;

        private String className;

        private String signature;

        private String superClass;

        private final List<String> interfaces;

        private BytecodeCreatorImpl enclosing;

        Builder() {
            superClass(Object.class);
            this.interfaces = new ArrayList<>();
        }

        Builder enclosing(BytecodeCreatorImpl enclosing) {
            this.enclosing = enclosing;
            return this;
        }

        public Builder classOutput(ClassOutput classOutput) {
            this.classOutput = classOutput;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder superClass(String superClass) {
            this.superClass = superClass;
            return this;
        }

        public Builder superClass(Class<?> superClass) {
            return superClass(superClass.getName());
        }

        public Builder interfaces(String... interfaces) {
            Collections.addAll(this.interfaces, interfaces);
            return this;
        }

        public Builder interfaces(Class<?>... interfaces) {
            for (Class<?> val : interfaces) {
                this.interfaces.add(val.getName());
            }
            return this;
        }

        public ClassCreator build() {
            Objects.requireNonNull(className);
            Objects.requireNonNull(classOutput);
            Objects.requireNonNull(superClass);
            return new ClassCreator(enclosing, classOutput, className, signature, superClass, interfaces.toArray(new String[0]));
        }

    }

}