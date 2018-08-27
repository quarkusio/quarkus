package org.jboss.protean.gizmo;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassCreator implements AutoCloseable {

    private final ClassOutput classOutput;
    private final String superClass;
    private final String[] interfaces;
    private final Map<MethodDescriptor, MethodCreatorImpl> methods = new HashMap<>();
    private final Map<FieldDescriptor, FieldCreatorImpl> fields = new HashMap<>();
    private final String className;

    public ClassCreator(ClassOutput classOutput, String name, Class<?> superClass, Class<?>... interfaces) {
        this.classOutput = classOutput;
        this.superClass = superClass.getName().replace(".", "/");
        this.interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            this.interfaces[i] = interfaces[i].getName().replace(".", "/");
        }
        this.className = name.replace(".", "/");
    }

    public ClassCreator(ClassOutput classOutput, String name, String superClass, String... interfaces) {
        this.classOutput = classOutput;
        this.superClass = superClass.replace(".", "/");
        this.interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            this.interfaces[i] = interfaces[i].replace(".", "/");
        }
        this.className = name.replace(".", "/");
    }

    public MethodCreator getMethodCreator(MethodDescriptor methodDescriptor) {
        if (methods.containsKey(methodDescriptor)) {
            return methods.get(methodDescriptor);
        }
        MethodCreatorImpl creator = new MethodCreatorImpl(methodDescriptor, className, classOutput, this);
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

    public void close() {
        ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String[] interfaces = new String[this.interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = this.interfaces[i];
        }
        file.visit(Opcodes.V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, className, null, superClass, interfaces);

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

        file.visitEnd();

        classOutput.write(className, file.toByteArray());
    }
}
