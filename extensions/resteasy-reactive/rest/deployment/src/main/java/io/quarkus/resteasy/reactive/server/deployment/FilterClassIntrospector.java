package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import jakarta.ws.rs.container.ResourceInfo;

import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

public class FilterClassIntrospector {

    private final ClassLoader classLoader;

    public FilterClassIntrospector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public boolean usesGetResourceMethod(MethodInfo methodInfo) {
        String className = methodInfo.declaringClass().name().toString();
        final String resourceName = fromClassNameToResourceName(className);
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            ClassReader configClassReader = new ClassReader(is);
            FilterClassVisitor classVisitor = new FilterClassVisitor(methodInfo.descriptor());
            configClassReader.accept(classVisitor, 0);
            return classVisitor.usesGetResourceMethod();
        } catch (IOException e) {
            throw new UncheckedIOException(className + " class reading failed", e);
        }
    }

    private static class FilterClassVisitor extends ClassVisitor {

        private final String methodDescriptor;
        private final UsesGetResourceMethodVisitor methodVisitor = new UsesGetResourceMethodVisitor();

        private FilterClassVisitor(String methodDescriptor) {
            super(Gizmo.ASM_API_VERSION);
            this.methodDescriptor = methodDescriptor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor superMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (methodDescriptor.equals(descriptor)) {
                return methodVisitor;
            }
            return superMethodVisitor;
        }

        public boolean usesGetResourceMethod() {
            return methodVisitor.usesGetResourceMethod;
        }
    }

    private static class UsesGetResourceMethodVisitor extends MethodVisitor {

        private boolean usesGetResourceMethod = false;

        private UsesGetResourceMethodVisitor() {
            super(Gizmo.ASM_API_VERSION);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if ((opcode == Opcodes.INVOKEINTERFACE) && ResourceInfo.class.getName().replace('.', '/').equals(owner)
                    && "getResourceMethod".equals(name)) {
                usesGetResourceMethod = true;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

}
