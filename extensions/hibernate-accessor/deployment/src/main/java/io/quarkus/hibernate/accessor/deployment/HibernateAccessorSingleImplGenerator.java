package io.quarkus.hibernate.accessor.deployment;

import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.TypeMetadata;
import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorGenerationUtil.SWITCH_CHUNK_SIZE;
import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorGenerationUtil.fqcnToName;
import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorGenerationUtil.pushIntConst;
import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorProcessor.ProcessedHostData;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

// Share one implementation class per accessor kind rather than generating a class for every member.
// Each instance stores a host index and a member index; dispatch then calls the injected host method,
// which has the access privileges needed to reach private members.
class HibernateAccessorSingleImplGenerator implements Opcodes, HibernateAccessorGeneratorConstants {

    byte[] generateReaderImpl(List<ProcessedHostData> hostClasses) {
        String className = fqcnToName(GENERATED_READER_IMPL);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, className,
                "Ljava/lang/Object;L" + READER_INTERFACE_INTERNAL + "<Ljava/lang/Object;>;",
                "java/lang/Object", new String[] { READER_INTERFACE_INTERNAL });

        generateIndexFields(cw);
        generateIndexConstructor(cw, className);

        String methodDesc = "(Ljava/lang/Object;)Ljava/lang/Object;";
        if (hostClasses.size() <= SWITCH_CHUNK_SIZE) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "get", methodDesc, null, null);
            mv.visitCode();
            generateDispatchSwitch(mv, className, hostClasses, PREFIX_READ_METHOD, "(ILjava/lang/Object;)Ljava/lang/Object;",
                    1);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            generateChunkedDispatch(cw, className, "get", methodDesc, hostClasses, PREFIX_READ_METHOD,
                    "(ILjava/lang/Object;)Ljava/lang/Object;", 1, false);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    byte[] generateWriterImpl(List<ProcessedHostData> hostClasses) {
        String className = fqcnToName(GENERATED_WRITER_IMPL);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, className,
                null,
                "java/lang/Object", new String[] { WRITER_INTERFACE_INTERNAL });

        generateIndexFields(cw);
        generateIndexConstructor(cw, className);

        if (hostClasses.size() <= SWITCH_CHUNK_SIZE) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "set",
                    "(Ljava/lang/Object;Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            generateWriteDispatchSwitch(mv, className, hostClasses);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            generateChunkedWriteDispatch(cw, className, hostClasses);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    byte[] generateInstantiatorImpl(List<ProcessedHostData> hostClasses) {
        String className = fqcnToName(GENERATED_INSTANTIATOR_IMPL);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, className,
                "Ljava/lang/Object;L" + INSTANTIATOR_INTERFACE_INTERNAL + "<Ljava/lang/Object;>;",
                "java/lang/Object", new String[] { INSTANTIATOR_INTERFACE_INTERNAL });

        generateIndexFields(cw);
        generateIndexConstructor(cw, className);

        String methodDesc = "([Ljava/lang/Object;)Ljava/lang/Object;";
        if (hostClasses.size() <= SWITCH_CHUNK_SIZE) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "create", methodDesc, null, null);
            mv.visitCode();
            generateDispatchSwitch(mv, className, hostClasses, PREFIX_CREATE_METHOD, "(I[Ljava/lang/Object;)Ljava/lang/Object;",
                    1);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            generateChunkedDispatch(cw, className, "create", methodDesc, hostClasses, PREFIX_CREATE_METHOD,
                    "(I[Ljava/lang/Object;)Ljava/lang/Object;", 1, false);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    // Generated fields shared by readers, writers and instantiators:
    // private final int classIndex;
    // private final int memberIndex;
    private static void generateIndexFields(ClassWriter cw) {
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "classIndex", "I", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "memberIndex", "I", null, null).visitEnd();
    }

    // Example generated constructor; writers and instantiators use their respective implementation names:
    // public QuarkusHibernateAccessorValueReaderImpl(int classIndex, int memberIndex) {
    //     super();
    //     this.classIndex = classIndex;
    //     this.memberIndex = memberIndex;
    // }
    private static void generateIndexConstructor(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(II)V", null, null);
        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "classIndex", "I");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitFieldInsn(PUTFIELD, className, "memberIndex", "I");

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Example generated code (names, types and indexes depend on the registered members):
    // public Object get(Object target) {
    //     switch (this.classIndex) {
    //         case 0: return Model.$$__hibernateAccessor_read(this.memberIndex, target);
    //         case 1: return OtherModel.$$__hibernateAccessor_read(this.memberIndex, target);
    //         default: throw new IllegalArgumentException("Unknown class index " + this.classIndex);
    //     }
    // }
    // Instantiator.create(Object... arguments) uses the same shape, calling each host's create method.
    // A non-public host is called through its public bridge class.
    private static void generateDispatchSwitch(MethodVisitor mv, String className, List<ProcessedHostData> hostClasses,
            String staticMethodName, String staticMethodDesc, int targetArgSlot) {
        int count = hostClasses.size();
        if (count == 0) {
            throwIllegalArgumentWithClassIndex(mv, className);
            return;
        }
        Label[] labels = new Label[count];
        for (int i = 0; i < count; i++) {
            labels[i] = new Label();
        }
        Label defaultLabel = new Label();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "classIndex", "I");
        mv.visitTableSwitchInsn(0, count - 1, defaultLabel, labels);

        for (int i = 0; i < count; i++) {
            mv.visitLabel(labels[i]);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            TypeMetadata type = hostClasses.get(i).type();

            String hostClass = fqcnToName(type.dispatchTarget());
            boolean isInterface = type.dispatchTargetIsInterface();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "memberIndex", "I");
            mv.visitVarInsn(ALOAD, targetArgSlot);
            mv.visitMethodInsn(INVOKESTATIC, hostClass, staticMethodName, staticMethodDesc, isInterface);
            mv.visitInsn(ARETURN);
        }

        mv.visitLabel(defaultLabel);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        throwIllegalArgumentWithClassIndex(mv, className);
    }

    // Example generated code (names, types and indexes depend on the registered members):
    // public void set(Object target, Object value) {
    //     switch (this.classIndex) {
    //         case 0: Model.$$__hibernateAccessor_write(this.memberIndex, target, value); return;
    //         case 1: OtherModel.$$__hibernateAccessor_write(this.memberIndex, target, value); return;
    //         default: throw new IllegalArgumentException("Unknown class index " + this.classIndex);
    //     }
    // }
    private static void generateWriteDispatchSwitch(MethodVisitor mv, String className, List<ProcessedHostData> hostClasses) {
        int count = hostClasses.size();
        if (count == 0) {
            throwIllegalArgumentWithClassIndex(mv, className);
            return;
        }
        Label[] labels = new Label[count];
        for (int i = 0; i < count; i++) {
            labels[i] = new Label();
        }
        Label defaultLabel = new Label();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "classIndex", "I");
        mv.visitTableSwitchInsn(0, count - 1, defaultLabel, labels);

        for (int i = 0; i < count; i++) {
            mv.visitLabel(labels[i]);
            mv.visitFrame(F_SAME, 0, null, 0, null);

            TypeMetadata type = hostClasses.get(i).type();
            String hostClass = fqcnToName(type.dispatchTarget());
            boolean isInterface = type.dispatchTargetIsInterface();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "memberIndex", "I");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, hostClass, PREFIX_WRITE_METHOD, "(ILjava/lang/Object;Ljava/lang/Object;)V",
                    isInterface);
            mv.visitInsn(RETURN);
        }

        mv.visitLabel(defaultLabel);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        throwIllegalArgumentWithClassIndex(mv, className);
    }

    private void generateChunkedDispatch(ClassWriter cw, String className, String publicMethodName, String publicMethodDesc,
            List<ProcessedHostData> hostClasses, String staticMethodName, String staticMethodDesc, int targetArgSlot,
            boolean returnsVoid) {
        int total = hostClasses.size();
        int chunkCount = (total + SWITCH_CHUNK_SIZE - 1) / SWITCH_CHUNK_SIZE;

        for (int chunk = 0; chunk < chunkCount; chunk++) {
            int start = chunk * SWITCH_CHUNK_SIZE;
            int end = Math.min(start + SWITCH_CHUNK_SIZE, total);
            List<ProcessedHostData> chunkHosts = hostClasses.subList(start, end);

            String chunkMethodName = publicMethodName + "$" + chunk;
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, chunkMethodName, publicMethodDesc, null, null);
            mv.visitCode();
            generateDispatchSwitchWithOffset(mv, className, chunkHosts, staticMethodName, staticMethodDesc, targetArgSlot,
                    start);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        generateImplChunkDispatcher(cw, className, publicMethodName, publicMethodDesc,
                chunkCount, returnsVoid);
    }

    private void generateChunkedWriteDispatch(ClassWriter cw, String className, List<ProcessedHostData> hostClasses) {
        int total = hostClasses.size();
        int chunkCount = (total + SWITCH_CHUNK_SIZE - 1) / SWITCH_CHUNK_SIZE;
        String publicMethodDesc = "(Ljava/lang/Object;Ljava/lang/Object;)V";

        for (int chunk = 0; chunk < chunkCount; chunk++) {
            int start = chunk * SWITCH_CHUNK_SIZE;
            int end = Math.min(start + SWITCH_CHUNK_SIZE, total);
            List<ProcessedHostData> chunkHosts = hostClasses.subList(start, end);

            String chunkMethodName = "set$" + chunk;
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, chunkMethodName, publicMethodDesc, null, null);
            mv.visitCode();
            generateWriteDispatchSwitchWithOffset(mv, className, chunkHosts, start);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        generateImplChunkDispatcher(cw, className, "set", publicMethodDesc, chunkCount, true);
    }

    // Example generated chunk helper with indexOffset = 1000:
    // private Object get$1(Object target) {
    //     switch (this.classIndex) {
    //         case 1000: return Model.$$__hibernateAccessor_read(this.memberIndex, target);
    //         // More hosts in this chunk, using their original class indexes...
    //         default: throw new IllegalArgumentException("Unknown class index " + this.classIndex);
    //     }
    // }
    // create$1(Object[] arguments) has the same shape, calling host instantiation methods.
    private static void generateDispatchSwitchWithOffset(MethodVisitor mv, String className,
            List<ProcessedHostData> hostClasses,
            String staticMethodName, String staticMethodDesc, int targetArgSlot, int indexOffset) {
        int count = hostClasses.size();
        Label[] labels = new Label[count];
        for (int i = 0; i < count; i++) {
            labels[i] = new Label();
        }
        Label defaultLabel = new Label();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "classIndex", "I");
        mv.visitTableSwitchInsn(indexOffset, indexOffset + count - 1, defaultLabel, labels);

        for (int i = 0; i < count; i++) {
            mv.visitLabel(labels[i]);
            mv.visitFrame(F_SAME, 0, null, 0, null);

            TypeMetadata type = hostClasses.get(i).type();
            String hostClass = fqcnToName(type.dispatchTarget());
            boolean isInterface = type.dispatchTargetIsInterface();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "memberIndex", "I");
            mv.visitVarInsn(ALOAD, targetArgSlot);
            mv.visitMethodInsn(INVOKESTATIC, hostClass, staticMethodName, staticMethodDesc, isInterface);
            mv.visitInsn(ARETURN);
        }

        mv.visitLabel(defaultLabel);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        throwIllegalArgumentWithClassIndex(mv, className);
    }

    // Example generated writer chunk helper with indexOffset = 1000:
    // private void set$1(Object target, Object value) {
    //     switch (this.classIndex) {
    //         case 1000: Model.$$__hibernateAccessor_write(this.memberIndex, target, value); return;
    //         // More hosts in this chunk, using their original class indexes...
    //         default: throw new IllegalArgumentException("Unknown class index " + this.classIndex);
    //     }
    // }
    private static void generateWriteDispatchSwitchWithOffset(MethodVisitor mv, String className,
            List<ProcessedHostData> hostClasses,
            int indexOffset) {
        int count = hostClasses.size();
        Label[] labels = new Label[count];
        for (int i = 0; i < count; i++) {
            labels[i] = new Label();
        }
        Label defaultLabel = new Label();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "classIndex", "I");
        mv.visitTableSwitchInsn(indexOffset, indexOffset + count - 1, defaultLabel, labels);

        for (int i = 0; i < count; i++) {
            mv.visitLabel(labels[i]);
            mv.visitFrame(F_SAME, 0, null, 0, null);

            TypeMetadata type = hostClasses.get(i).type();
            String hostClass = fqcnToName(type.dispatchTarget());
            boolean isInterface = type.dispatchTargetIsInterface();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "memberIndex", "I");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, hostClass, PREFIX_WRITE_METHOD, "(ILjava/lang/Object;Ljava/lang/Object;)V",
                    isInterface);
            mv.visitInsn(RETURN);
        }

        mv.visitLabel(defaultLabel);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        throwIllegalArgumentWithClassIndex(mv, className);
    }

    // Generated implementation dispatcher; SWITCH_CHUNK_SIZE is embedded as an integer constant:
    // public Object get(Object target) {
    //     switch (this.classIndex / SWITCH_CHUNK_SIZE) {
    //         case 0: return this.get$0(target);
    //         case 1: return this.get$1(target);
    //         default: throw new IllegalArgumentException("Unknown class index " + this.classIndex);
    //     }
    // }
    // create forwards its argument array; set forwards target and value, then returns void.
    // Helpers check the original class index, including negative or out-of-range indexes.
    private static void generateImplChunkDispatcher(ClassWriter cw, String className, String publicMethodName,
            String publicMethodDesc, int chunkCount, boolean returnsVoid) {
        int accessFlags = ACC_PUBLIC;
        if (publicMethodDesc.startsWith("([")) {
            accessFlags |= ACC_VARARGS;
        }
        MethodVisitor mv = cw.visitMethod(accessFlags, publicMethodName, publicMethodDesc, null, null);
        mv.visitCode();

        Label[] labels = new Label[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            labels[i] = new Label();
        }
        Label defaultLabel = new Label();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "classIndex", "I");
        pushIntConst(mv, SWITCH_CHUNK_SIZE);
        mv.visitInsn(IDIV);
        mv.visitTableSwitchInsn(0, chunkCount - 1, defaultLabel, labels);

        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(publicMethodDesc);

        for (int i = 0; i < chunkCount; i++) {
            mv.visitLabel(labels[i]);
            mv.visitFrame(F_SAME, 0, null, 0, null);

            mv.visitVarInsn(ALOAD, 0);
            for (int a = 0, slot = 1; a < argTypes.length; a++) {
                mv.visitVarInsn(argTypes[a].getOpcode(ILOAD), slot);
                slot += argTypes[a].getSize();
            }

            mv.visitMethodInsn(INVOKEVIRTUAL, className, publicMethodName + "$" + i, publicMethodDesc, false);

            if (returnsVoid) {
                mv.visitInsn(RETURN);
            } else {
                mv.visitInsn(ARETURN);
            }
        }

        mv.visitLabel(defaultLabel);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        throwIllegalArgumentWithClassIndex(mv, className);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Generated failure block:
    // throw new IllegalArgumentException("Unknown class index " + this.classIndex);
    private static void throwIllegalArgumentWithClassIndex(MethodVisitor mv, String implClassName) {
        mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
        mv.visitInsn(DUP);
        // message = "Unknown class index " + this.classIndex
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Unknown class index ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, implClassName, "classIndex", "I");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
    }

}
