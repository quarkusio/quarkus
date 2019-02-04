package org.jboss.shamrock.panache;

import static org.jboss.shamrock.panache.PanacheRxModelEnhancer.RX_ENTITY_BASE_SIGNATURE;
import static org.jboss.shamrock.panache.PanacheRxModelEnhancer.RX_MODEL_FIELD_NAME;
import static org.jboss.shamrock.panache.PanacheRxModelEnhancer.RX_MODEL_INFO_SIGNATURE;
import static org.jboss.shamrock.panache.PanacheRxModelEnhancer.RX_OPERATIONS_BINARY_NAME;

import java.util.function.BiFunction;

import org.jboss.panache.rx.RxDaoBase;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

public class PanacheRxDaoEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String RX_DAO_BASE_NAME = RxDaoBase.class.getName();
    public final static String RX_DAO_BASE_BINARY_NAME = RX_DAO_BASE_NAME.replace('.', '/');
    public final static String RX_DAO_BASE_SIGNATURE = "L"+RX_DAO_BASE_BINARY_NAME+";";

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new RxDaoEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class RxDaoEnhancingClassVisitor extends ClassVisitor {

        private String entityBinaryType;
        private String rxModelInfoType;
        private String rxModelInfoDesc;

        public RxDaoEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            SignatureReader signatureReader = new SignatureReader(signature);
            DaoTypeFetcher daoTypeFetcher = new DaoTypeFetcher(RX_DAO_BASE_BINARY_NAME);
            signatureReader.accept(daoTypeFetcher);
            entityBinaryType = daoTypeFetcher.foundType;
            
            rxModelInfoType = entityBinaryType + PanacheRxModelEnhancer.RX_MODEL_SUFFIX;
            rxModelInfoDesc = "L"+rxModelInfoType+";";
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // FIXME: do not add method if already present 
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        
        @Override
        public void visitEnd() {
            // findById
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "findById", 
                                                 "(Ljava/lang/Object;)Lio/reactivex/Maybe;", 
                                                 "(Ljava/lang/Object;)Lio/reactivex/Maybe<"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "findById", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/Object;)Lio/reactivex/Maybe;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // find
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "find", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable;", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable<"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitIntInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "find", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // findAll
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "findAll", 
                                                 "()Lio/reactivex/Observable;", 
                                                 "()Lio/reactivex/Observable<"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "findAll", 
                               "("+RX_MODEL_INFO_SIGNATURE+")Lio/reactivex/Observable;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            
            // count
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "count", 
                                                 "()Lio/reactivex/Single;", 
                                                 "()Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "count", 
                               "("+RX_MODEL_INFO_SIGNATURE+")Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // count
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "count", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitIntInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "count", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // deleteAll
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "deleteAll", 
                                                 "()Lio/reactivex/Single;", 
                                                 "()Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "deleteAll", 
                               "("+RX_MODEL_INFO_SIGNATURE+")Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // delete
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                 "delete", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitIntInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "delete", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            super.visitEnd();
            
        }
    }
}
