package org.jboss.protean.gizmo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MethodCreatorImpl extends BytecodeCreatorImpl implements MethodCreator {

    private int modifiers = Opcodes.ACC_PUBLIC;
    private final List<String> exceptions = new ArrayList<>();
    private final List<AnnotationCreatorImpl> annotations = new ArrayList<>();
    private final Map<Integer, AnnotationParameters> parameterAnnotations = new HashMap<>();
    private final Map<MethodDescriptor, MethodDescriptor> superclassAccessors = new HashMap<>();
    private static final AtomicInteger accessorCount = new AtomicInteger();

    private final MethodDescriptor methodDescriptor;
    private final String declaringClassName;
    private final ClassOutput classOutput;
    private final ClassCreator classCreator;

    MethodCreatorImpl(MethodDescriptor methodDescriptor, String declaringClassName, ClassOutput classOutput, ClassCreator classCreator) {
        super();
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

    MethodDescriptor getSuperclassAccessor(MethodDescriptor descriptor) {
        if (superclassAccessors.containsKey(descriptor)) {
            return superclassAccessors.get(descriptor);
        }
        String name = descriptor.getName() + "$$superaccessor" + accessorCount.incrementAndGet();
        MethodCreator ctor = getClassCreator().getMethodCreator(name, descriptor.getReturnType(), descriptor.getParameterTypes());
        ResultHandle[] params = new ResultHandle[descriptor.getParameterTypes().length];
        for (int i = 0; i < params.length; ++i) {
            params[i] = ctor.getMethodParam(i);
        }
        ResultHandle ret = ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(getClassCreator().getSuperClass(), descriptor.getName(), descriptor.getReturnType(), descriptor.getParameterTypes()), ctor.getThis(), params);
        ctor.returnValue(ret);
        superclassAccessors.put(descriptor, ctor.getMethodDescriptor());
        return ctor.getMethodDescriptor();
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
