package io.quarkus.resteasy.reactive.server.deployment;

import java.util.function.BiFunction;

import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionContext;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionTarget;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

class MultipartTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final String THREAD_BINARY_NAME = "java/lang/Thread";
    private static final String CLASS_BINARY_NAME = "java/lang/Class";
    private static final String INJECTION_TARGET_BINARY_NAME = ResteasyReactiveInjectionTarget.class.getName()
            .replace('.', '/');
    private static final String INJECTION_CONTEXT_BINARY_NAME = ResteasyReactiveInjectionContext.class.getName()
            .replace('.', '/');
    private static final String INJECTION_CONTEXT_DESCRIPTOR = "L" + INJECTION_CONTEXT_BINARY_NAME + ";";

    private static final String INJECT_METHOD_NAME = "__quarkus_rest_inject";
    private static final String INJECT_METHOD_DESCRIPTOR = "(" + INJECTION_CONTEXT_DESCRIPTOR + ")V";

    private final String populatorName;

    public MultipartTransformer(String populatorName) {
        this.populatorName = populatorName;
    }

    @Override
    public ClassVisitor apply(String s, ClassVisitor visitor) {
        return new MultipartClassVisitor(Gizmo.ASM_API_VERSION, visitor, populatorName);
    }

    static class MultipartClassVisitor extends ClassVisitor {

        private String thisDescriptor;
        private final String populatorName;

        public MultipartClassVisitor(int api, ClassVisitor classVisitor, String populatorName) {
            super(api, classVisitor);
            this.populatorName = populatorName;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            thisDescriptor = "L" + name + ";";

            // make the class public
            access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
            access |= Opcodes.ACC_PUBLIC;

            String[] newInterfaces = new String[interfaces.length + 1];
            newInterfaces[0] = INJECTION_TARGET_BINARY_NAME;
            System.arraycopy(interfaces, 0, newInterfaces, 1, interfaces.length);
            super.visit(version, access, name, signature, superName, newInterfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (((access & Opcodes.ACC_FINAL) == 0) && ((access & Opcodes.ACC_STATIC) == 0)) {
                // convert non-final, non-static fields to public so our generated code can always access it
                access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
                access |= Opcodes.ACC_PUBLIC;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public void visitEnd() {
            MethodVisitor injectMethod = visitMethod(Opcodes.ACC_PUBLIC, INJECT_METHOD_NAME, INJECT_METHOD_DESCRIPTOR, null,
                    null);
            injectMethod.visitParameter("ctx", 0 /* modifiers */);
            injectMethod.visitCode();

            // this
            injectMethod.visitIntInsn(Opcodes.ALOAD, 0);
            // ctx param
            injectMethod.visitIntInsn(Opcodes.ALOAD, 1);

            // Call the populator using reflection. This is needed to ensure that the populator is loaded from the proper classloader
            // See: https://github.com/quarkusio/quarkus/issues/19037
            // All this ASM essentially generates:
            //Thread.currentThread().getContextClassLoader().loadClass(populatorName).getMethod("populate", this.getClass(), ResteasyReactiveInjectionContext.class).invoke((Object)null, this, ctx);

            injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, THREAD_BINARY_NAME, "currentThread", "()Ljava/lang/Thread;",
                    false);
            injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREAD_BINARY_NAME, "getContextClassLoader",
                    "()Ljava/lang/ClassLoader;", false);
            injectMethod.visitLdcInsn(populatorName);
            injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass",
                    "(Ljava/lang/String;)Ljava/lang/Class;", false);
            injectMethod.visitLdcInsn("populate");
            injectMethod.visitInsn(Opcodes.ICONST_2);
            injectMethod.visitTypeInsn(Opcodes.ANEWARRAY, CLASS_BINARY_NAME);
            injectMethod.visitInsn(Opcodes.DUP);
            injectMethod.visitInsn(Opcodes.ICONST_0);
            injectMethod.visitVarInsn(Opcodes.ALOAD, 0);
            injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            injectMethod.visitInsn(Opcodes.AASTORE);
            injectMethod.visitInsn(Opcodes.DUP);
            injectMethod.visitInsn(Opcodes.ICONST_1);
            injectMethod.visitLdcInsn(Type.getType(INJECTION_CONTEXT_DESCRIPTOR));
            injectMethod.visitInsn(Opcodes.AASTORE);
            injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CLASS_BINARY_NAME, "getMethod",
                    "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
            injectMethod.visitInsn(Opcodes.ACONST_NULL);
            injectMethod.visitInsn(Opcodes.ICONST_2);
            injectMethod.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            injectMethod.visitInsn(Opcodes.DUP);
            injectMethod.visitInsn(Opcodes.ICONST_0);
            injectMethod.visitVarInsn(Opcodes.ALOAD, 0);
            injectMethod.visitInsn(Opcodes.AASTORE);
            injectMethod.visitInsn(Opcodes.DUP);
            injectMethod.visitInsn(Opcodes.ICONST_1);
            injectMethod.visitVarInsn(Opcodes.ALOAD, 1);
            injectMethod.visitInsn(Opcodes.AASTORE);
            injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
            injectMethod.visitInsn(Opcodes.RETURN);
            injectMethod.visitEnd();
            injectMethod.visitMaxs(0, 0);
        }
    }
}
