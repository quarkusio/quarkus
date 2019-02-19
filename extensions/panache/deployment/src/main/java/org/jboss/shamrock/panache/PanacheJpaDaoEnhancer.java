package org.jboss.shamrock.panache;

import static org.jboss.shamrock.panache.PanacheJpaModelEnhancer.JPA_OPERATIONS_BINARY_NAME;
import static org.jboss.shamrock.panache.PanacheJpaModelEnhancer.QUERY_BINARY_NAME;
import static org.jboss.shamrock.panache.PanacheJpaModelEnhancer.QUERY_SIGNATURE;

import java.util.function.BiFunction;

import org.jboss.panache.jpa.DaoBase;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

public class PanacheJpaDaoEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String DAO_BASE_NAME = DaoBase.class.getName();
    public final static String DAO_BASE_BINARY_NAME = DAO_BASE_NAME.replace('.', '/');
    public final static String DAO_BASE_SIGNATURE = "L"+DAO_BASE_BINARY_NAME+";";

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new DaoEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class DaoEnhancingClassVisitor extends ClassVisitor {

        private Type entityType;
        private String entitySignature;
        private String entityBinaryType;
        private String daoBinaryName;

        public DaoEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
            daoBinaryName = className.replace('.', '/');
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            SignatureReader signatureReader = new SignatureReader(signature);
            DaoTypeFetcher daoTypeFetcher = new DaoTypeFetcher(DAO_BASE_BINARY_NAME);
            signatureReader.accept(daoTypeFetcher);
            entityBinaryType = daoTypeFetcher.foundType;
            entitySignature = "L"+entityBinaryType+";";
            entityType = Type.getType(entitySignature);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // FIXME: do not add method if already present
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "findById",
                    "(Ljava/lang/Object;)"+entitySignature,
                    null,
                    null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "findById",
                    "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(Opcodes.CHECKCAST, entityBinaryType);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // Bridge for findById
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                    "findById",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    null,
                    null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                               daoBinaryName,
                               "findById",
                               "(Ljava/lang/Object;)"+entitySignature, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "find",
                    "(Ljava/lang/String;[Ljava/lang/Object;)"+QUERY_SIGNATURE,
                    "(Ljava/lang/String;[Ljava/lang/Object;)L"+QUERY_BINARY_NAME+"<"+entitySignature+">;",
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitIntInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "find",
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)"+QUERY_SIGNATURE, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "findAll",
                    "()"+QUERY_SIGNATURE,
                    "()L"+QUERY_BINARY_NAME+"<"+entitySignature+">;",
                    null);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "findAll",
                    "(Ljava/lang/Class;)"+QUERY_SIGNATURE, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "count",
                    "()J",
                    null,
                    null);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "count",
                    "(Ljava/lang/Class;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "count",
                    "(Ljava/lang/String;[Ljava/lang/Object;)J",
                    null,
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitIntInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "count",
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "delete",
                    "(Ljava/lang/String;[Ljava/lang/Object;)J",
                    null,
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitIntInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "delete",
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "deleteAll",
                    "()J",
                    null,
                    null);
            mv.visitCode();
            mv.visitLdcInsn(entityType);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "deleteAll",
                    "(Ljava/lang/Class;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            super.visitEnd();
        }
    }
}
