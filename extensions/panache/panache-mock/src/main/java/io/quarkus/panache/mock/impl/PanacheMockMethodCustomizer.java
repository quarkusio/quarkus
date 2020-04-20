package io.quarkus.panache.mock.impl;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.panache.common.deployment.JandexUtil;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.mock.PanacheMock;

public class PanacheMockMethodCustomizer implements PanacheMethodCustomizer {

    private final static String PANACHE_MOCK_BINARY_NAME = PanacheMock.class.getName().replace('.', '/');
    private final static String PANACHE_MOCK_INVOKE_REAL_METHOD_EXCEPTION_BINARY_NAME = PanacheMock.InvokeRealMethodException.class
            .getName().replace('.', '/');

    @Override
    public void customize(Type entityClassSignature, MethodInfo method, MethodVisitor mv) {
        /*
         * Generated code:
         * 
         * if(PanacheMock.IsMockEnabled && PanacheMock.isMocked(TestClass.class)) {
         * try {
         * return (int)PanacheMock.mockMethod(TestClass.class, "foo", new Class<?>[] {int.class}, new Object[] {arg});
         * } catch (PanacheMock.InvokeRealMethodException e) {
         * // fall-through
         * }
         * }
         * 
         * Bytecode approx:
         * 
         * 0: getstatic #16 // Field PanacheMock.IsMockEnabled:Z
         * 3: ifeq 50
         * 6: ldc #1 // class MyTestMockito$TestClass
         * 8: invokestatic #22 // Method PanacheMock.isMocked:(Ljava/lang/Class;)Z
         * 11: ifeq 50
         * 14: ldc #1 // class MyTestMockito$TestClass
         * 16: ldc #26 // String foo
         * 
         * 18: iconst_1
         * 19: anewarray #27 // class java/lang/Class
         * 22: dup
         * 23: iconst_0
         * 24: getstatic #29 // Field java/lang/Integer.TYPE:Ljava/lang/Class;
         * 27: aastore
         *
         * 28: iconst_1
         * 29: anewarray #3 // class java/lang/Object
         * 32: dup
         * 33: iconst_0
         * 34: iload_0
         * 35: invokestatic #35 // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
         * 38: aastore
         * 
         * 39: invokestatic #39 // Method
         * PanacheMock.mockMethod:(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;
         * 42: checkcast #30 // class java/lang/Integer
         * 45: invokevirtual #43 // Method java/lang/Integer.intValue:()I
         * 48: ireturn
         * 49: astore_1
         */
        Label realMethodLabel = new Label();

        mv.visitFieldInsn(Opcodes.GETSTATIC, PANACHE_MOCK_BINARY_NAME, "IsMockEnabled", "Z");
        mv.visitJumpInsn(Opcodes.IFEQ, realMethodLabel);

        mv.visitLdcInsn(entityClassSignature);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PANACHE_MOCK_BINARY_NAME, "isMocked", "(Ljava/lang/Class;)Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, realMethodLabel);

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label tryHandler = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, tryHandler, PANACHE_MOCK_INVOKE_REAL_METHOD_EXCEPTION_BINARY_NAME);
        mv.visitLabel(tryStart);

        mv.visitLdcInsn(entityClassSignature);
        mv.visitLdcInsn(method.name());

        mv.visitLdcInsn(method.parameters().size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");

        int i = 0;
        for (org.jboss.jandex.Type paramType : method.parameters()) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(i);
            JandexUtil.visitLdc(mv, paramType);
            mv.visitInsn(Opcodes.AASTORE);
            i++;
        }

        mv.visitLdcInsn(method.parameters().size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        i = 0;
        int paramSlot = 0;
        for (org.jboss.jandex.Type paramType : method.parameters()) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(i);
            mv.visitVarInsn(JandexUtil.getLoadOpcode(paramType), paramSlot);
            JandexUtil.boxIfRequired(mv, paramType);
            mv.visitInsn(Opcodes.AASTORE);
            i++;
            paramSlot += JandexUtil.getParameterSize(paramType);
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PANACHE_MOCK_BINARY_NAME, "mockMethod",
                "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        JandexUtil.unboxIfRequired(mv, method.returnType());
        if (method.returnType().kind() != Kind.PRIMITIVE) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, method.returnType().name().toString('/'));
        }

        mv.visitInsn(JandexUtil.getReturnInstruction(method.returnType()));

        mv.visitLabel(tryHandler);
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(tryEnd);

        mv.visitLabel(realMethodLabel);
    }
}
