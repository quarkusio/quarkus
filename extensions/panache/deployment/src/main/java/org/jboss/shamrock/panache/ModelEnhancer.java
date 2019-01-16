package org.jboss.shamrock.panache;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ModelEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class ModelEnhancingClassVisitor extends ClassVisitor {

        private Type thisClass;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
            thisClass = Type.getType("L"+className.replace('.', '/')+";");
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // FIXME: do not add method if already present 
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "findById", 
                    "(Ljava/lang/Object;)Lorg/jboss/panache/EntityBase;", 
                    "<T:Lorg/jboss/panache/EntityBase;>(Ljava/lang/Object;)TT;", 
                    null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "findById", 
                    "(Ljava/lang/Class;Ljava/lang/Object;)Lorg/jboss/panache/EntityBase;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "find", 
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List;", 
                    "<T:Lorg/jboss/panache/EntityBase;>(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List<TT;>;", 
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "find", 
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "findAll", 
                    "()Ljava/util/List;", 
                    "<T:Lorg/jboss/panache/EntityBase;>()Ljava/util/List<TT;>;", 
                    null);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "findAll", 
                    "(Ljava/lang/Class;)Ljava/util/List;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "count", 
                    "()J", 
                    null, 
                    null);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "count", 
                    "(Ljava/lang/Class;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "count", 
                    "(Ljava/lang/String;[Ljava/lang/Object;)J", 
                    null, 
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "count", 
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "delete", 
                    "(Ljava/lang/String;[Ljava/lang/Object;)J", 
                    null, 
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "delete", 
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "deleteAll", 
                    "()J", 
                    null, 
                    null);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    "org/jboss/panache/EntityBase", 
                    "deleteAll", 
                    "(Ljava/lang/Class;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            super.visitEnd();
        }
    }
}
