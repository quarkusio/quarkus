package org.jboss.protean.gizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MethodCreatorImpl extends BytecodeCreatorImpl implements MethodCreator {

    private int modifiers = Opcodes.ACC_PUBLIC;
    private final List<String> exceptions = new ArrayList<>();

    MethodCreatorImpl(MethodDescriptor methodDescriptor, String declaringClassName, ClassOutput classOutput, ClassCreator classCreator) {
        super(methodDescriptor, declaringClassName, new AtomicInteger(1), classOutput, classCreator);

        for (int i = 0; i < methodDescriptor.getParameterTypes().length; ++i) {
            String s = methodDescriptor.getParameterTypes()[i];
            if (s.equals("J") || s.equals("D")) {
                localVarCount.addAndGet(2);
            } else {
                localVarCount.incrementAndGet();
            }
        }
    }

    @Override
    public MethodCreator addException(String exception) {
        exceptions.add(exception.replace(".", "/"));
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
        writeOperations(visitor);
        visitor.visitMaxs(0, localVarCount.get());
        visitor.visitEnd();
    }

    @Override
    public String toString() {
        return "MethodCreatorImpl [declaringClassName=" + declaringClassName + ", methodDescriptor=" + methodDescriptor + "]";
    }

}
