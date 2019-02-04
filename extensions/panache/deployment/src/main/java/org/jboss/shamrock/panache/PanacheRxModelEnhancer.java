package org.jboss.shamrock.panache;

import java.util.function.BiFunction;

import org.jboss.panache.rx.RxEntityBase;
import org.jboss.panache.rx.RxModel;
import org.jboss.panache.rx.RxModelInfo;
import org.jboss.panache.rx.RxOperations;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PanacheRxModelEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String RX_ENTITY_BASE_NAME = RxEntityBase.class.getName();
    public final static String RX_ENTITY_BASE_BINARY_NAME = RX_ENTITY_BASE_NAME.replace('.', '/');
    public final static String RX_ENTITY_BASE_SIGNATURE = "L"+RX_ENTITY_BASE_BINARY_NAME+";";

    public final static String RX_OPERATIONS_NAME = RxOperations.class.getName();
    public final static String RX_OPERATIONS_BINARY_NAME = RX_OPERATIONS_NAME.replace('.', '/');
    public final static String RX_OPERATIONS_SIGNATURE = "L"+RX_OPERATIONS_BINARY_NAME+";";

    public final static String RX_MODEL_INFO_NAME = RxModelInfo.class.getName();
    public final static String RX_MODEL_INFO_BINARY_NAME = RX_MODEL_INFO_NAME.replace('.', '/');
    public final static String RX_MODEL_INFO_SIGNATURE = "L"+RX_MODEL_INFO_BINARY_NAME+";";

    public final static String RX_MODEL_NAME = RxModel.class.getName();
    public final static String RX_MODEL_BINARY_NAME = RX_MODEL_NAME.replace('.', '/');
    public final static String RX_MODEL_SIGNATURE = "L"+RX_MODEL_BINARY_NAME+";";

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class ModelEnhancingClassVisitor extends ClassVisitor {

        private String thisName;
        private boolean defaultConstructorPresent;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
            thisName = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if("<init>".equals(name) && "()V".equals(descriptor))
                defaultConstructorPresent = true;
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        
        @Override
        public void visitEnd() {
            // no-arg constructor 
            // FIXME: should actually use the proper superclass
            MethodVisitor mv;
            if(!defaultConstructorPresent) {
                mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                       "<init>", 
                                       "()V", 
                                       null, 
                                       null);
                mv.visitCode();
                mv.visitIntInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
                                   RX_MODEL_BINARY_NAME, 
                                   "<init>", 
                                   "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            // model field
            String fieldName = "$$MODEL";
            String modelName = thisName + "$__MODEL";
            String modelType = modelName.replace('.', '/');
            String modelDesc = "L"+modelType+";";
            super.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, fieldName, modelDesc, null, null).visitEnd();
            
            // model field init
            MethodVisitor staticInit = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "<clinit>", "()V", null, null);
            staticInit.visitTypeInsn(Opcodes.NEW, modelType);
            staticInit.visitInsn(Opcodes.DUP);
            staticInit.visitMethodInsn(Opcodes.INVOKESPECIAL, 
                                       modelType, 
                                       "<init>", 
                                       "()V", false);
            staticInit.visitFieldInsn(Opcodes.PUTSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            staticInit.visitInsn(Opcodes.RETURN);
            staticInit.visitMaxs(0, 0);
            staticInit.visitEnd();
            
            // getModelInfo
            mv = super.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_SYNTHETIC, 
                                                 "getModelInfo", 
                                                 "()"+RX_MODEL_INFO_SIGNATURE+"", 
                                                 "()L"+RX_ENTITY_BASE_BINARY_NAME+"<+"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            
            // findById
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "findById", 
                                                 "(Ljava/lang/Object;)Lio/reactivex/Maybe;", 
                                                 "(Ljava/lang/Object;)Lio/reactivex/Maybe<"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "findById", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/Object;)Lio/reactivex/Maybe;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // find
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "find", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable;", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable<"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "find", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // findAll
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "findAll", 
                                                 "()Lio/reactivex/Observable;", 
                                                 "()Lio/reactivex/Observable<"+RX_ENTITY_BASE_SIGNATURE+">;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "findAll", 
                               "("+RX_MODEL_INFO_SIGNATURE+")Lio/reactivex/Observable;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            
            // count
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "count", 
                                                 "()Lio/reactivex/Single;", 
                                                 "()Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "count", 
                               "("+RX_MODEL_INFO_SIGNATURE+")Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // count
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "count", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "count", 
                               "("+RX_MODEL_INFO_SIGNATURE+"Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // deleteAll
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "deleteAll", 
                                                 "()Lio/reactivex/Single;", 
                                                 "()Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                               RX_OPERATIONS_BINARY_NAME, 
                               "deleteAll", 
                               "("+RX_MODEL_INFO_SIGNATURE+")Lio/reactivex/Single;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            // delete
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                                                 "delete", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;", 
                                                 "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single<Ljava/lang/Long;>;", 
                                                 null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, thisName.replace('.', '/'), fieldName, modelDesc);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
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
