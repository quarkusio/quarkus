package io.quarkus.hibernate.accessor.deployment;

import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorGenerationUtil.fqcnToName;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class HibernateAccessorBridgeGenerator implements Opcodes, HibernateAccessorGeneratorConstants {

    private static final String BRIDGE_SUFFIX = "$$HibernateAccessorBridge";

    static String bridgeFqcn(String hostFqcn) {
        return hostFqcn + BRIDGE_SUFFIX;
    }

    byte[] generate(String hostFqcn, boolean hostIsInterface) {
        String bridgeName = fqcnToName(bridgeFqcn(hostFqcn));
        String hostName = fqcnToName(hostFqcn);

        ClassWriter cw = new ClassWriter(0);
        cw.visit(V17, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, bridgeName,
                null, "java/lang/Object", null);

        generateForward(cw, PREFIX_READ_METHOD, "(ILjava/lang/Object;)Ljava/lang/Object;", hostName, hostIsInterface);
        generateForward(cw, PREFIX_WRITE_METHOD, "(ILjava/lang/Object;Ljava/lang/Object;)V", hostName, hostIsInterface);
        generateForward(cw, PREFIX_CREATE_METHOD, "(I[Ljava/lang/Object;)Ljava/lang/Object;", hostName, hostIsInterface);

        // Always include all accessor method forwards for bridge
        generateForward(cw, METHOD_NAME_FIELD_READER_ACCESSOR, accessorMethodDescriptor(READER_INTERFACE_INTERNAL), hostName,
                hostIsInterface);
        generateForward(cw, METHOD_NAME_METHOD_READER_ACCESSOR, accessorMethodDescriptor(READER_INTERFACE_INTERNAL), hostName,
                hostIsInterface);
        generateForward(cw, METHOD_NAME_FIELD_WRITER_ACCESSOR, accessorMethodDescriptor(WRITER_INTERFACE_INTERNAL), hostName,
                hostIsInterface);
        generateForward(cw, METHOD_NAME_METHOD_WRITER_ACCESSOR, accessorMethodDescriptor(WRITER_INTERFACE_INTERNAL), hostName,
                hostIsInterface);
        generateForward(cw, METHOD_NAME_INSTANTIATOR_ACCESSOR, accessorMethodDescriptor(INSTANTIATOR_INTERFACE_INTERNAL),
                hostName, hostIsInterface);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static String accessorMethodDescriptor(String name) {
        return "(Ljava/lang/String;)L" + name + ";";
    }

    // Example methods generated in Model$$HibernateAccessorBridge, in the same package as Model:
    // public static Object $$__hibernateAccessor_read(int memberIndex, Object target) {
    //     return Model.$$__hibernateAccessor_read(memberIndex, target);
    // }
    // public static void $$__hibernateAccessor_write(int memberIndex, Object target, Object value) {
    //     Model.$$__hibernateAccessor_write(memberIndex, target, value);
    // }
    // Creation and accessor lookup methods similarly forward all arguments and return the host's result.
    private static void generateForward(ClassWriter cw, String methodName, String descriptor,
            String hostName, boolean hostIsInterface) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, methodName, descriptor, null, null);
        mv.visitCode();

        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(descriptor);
        for (int i = 0, slot = 0; i < argTypes.length; i++) {
            mv.visitVarInsn(argTypes[i].getOpcode(ILOAD), slot);
            slot += argTypes[i].getSize();
        }

        mv.visitMethodInsn(INVOKESTATIC, hostName, methodName, descriptor, hostIsInterface);

        org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getReturnType(descriptor);
        mv.visitInsn(returnType.getOpcode(IRETURN));

        mv.visitMaxs(argTypes.length, argTypes.length);
        mv.visitEnd();
    }
}
