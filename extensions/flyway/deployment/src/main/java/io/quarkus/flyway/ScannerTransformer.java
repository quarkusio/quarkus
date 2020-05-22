package io.quarkus.flyway;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.function.BiFunction;

import org.flywaydb.core.internal.scanner.Scanner;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.flyway.runtime.QuarkusPathLocationScanner;
import io.quarkus.gizmo.Gizmo;

/**
 * Transforms {@link Scanner} in a way to take advantage of our build time knowledge
 * This should be removed completely if https://github.com/flyway/flyway/issues/2822
 * is implemented
 */
class ScannerTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    static final String FLYWAY_SCANNER_CLASS_NAME = Scanner.class.getName();
    private static final String FLYWAY_SCANNER_INTERNAL_CLASS_NAME = FLYWAY_SCANNER_CLASS_NAME.replace('.', '/');

    private static final String FLYWAY_RESOURCE_AND_CLASS_SCANNER_CLASS_NAME = ResourceAndClassScanner.class.getName();
    private static final String FLYWAY_RESOURCE_AND_CLASS_SCANNER_INTERNAL_CLASS_NAME = FLYWAY_RESOURCE_AND_CLASS_SCANNER_CLASS_NAME
            .replace('.', '/');

    private static final String QUARKUS_RESOURCE_AND_CLASS_SCANNER_CLASS_NAME = QuarkusPathLocationScanner.class.getName();
    private static final String QUARKUS_RESOURCE_AND_CLASS_SCANNER_INTERNAL_CLASS_NAME = QUARKUS_RESOURCE_AND_CLASS_SCANNER_CLASS_NAME
            .replace('.', '/');

    private static final String CTOR_METHOD_NAME = "<init>";

    @Override
    public ClassVisitor apply(String s, ClassVisitor cv) {
        return new ScannerVisitor(cv);
    }

    private static final class ScannerVisitor extends ClassVisitor {

        public ScannerVisitor(ClassVisitor cv) {
            super(Gizmo.ASM_API_VERSION, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals(CTOR_METHOD_NAME)) {
                return new ConstructorTransformer(mv);
            }
            return mv;
        }

        /**
         * Replaces the constructor of the {@link Scanner} with:
         *
         * <pre>
         * public ScannerSubstitutions(Class<?> implementedInterface, Collection<Location> locations, ClassLoader classLoader,
         *         Charset encoding, ResourceNameCache resourceNameCache, LocationScannerCache locationScannerCache) {
         *     ResourceAndClassScanner quarkusScanner = new QuarkusPathLocationScanner(locations);
         *     resources.addAll(quarkusScanner.scanForResources());
         *     classes.addAll(quarkusScanner.scanForClasses());
         * }
         * </pre>
         */
        private static class ConstructorTransformer extends MethodVisitor {

            public ConstructorTransformer(MethodVisitor mv) {
                super(Gizmo.ASM_API_VERSION, mv);
            }

            @Override
            public void visitCode() {
                super.visitVarInsn(ALOAD, 0);
                super.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", CTOR_METHOD_NAME, "()V", false);
                super.visitVarInsn(ALOAD, 0);
                super.visitTypeInsn(NEW, "java/util/ArrayList");
                super.visitInsn(DUP);
                super.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", CTOR_METHOD_NAME, "()V", false);
                super.visitFieldInsn(PUTFIELD, FLYWAY_SCANNER_INTERNAL_CLASS_NAME, "resources", "Ljava/util/List;");
                super.visitVarInsn(ALOAD, 0);
                super.visitTypeInsn(NEW, "java/util/ArrayList");
                super.visitInsn(DUP);
                super.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", CTOR_METHOD_NAME, "()V", false);
                super.visitFieldInsn(PUTFIELD, FLYWAY_SCANNER_INTERNAL_CLASS_NAME, "classes", "Ljava/util/List;");
                super.visitTypeInsn(NEW, QUARKUS_RESOURCE_AND_CLASS_SCANNER_INTERNAL_CLASS_NAME);
                super.visitInsn(DUP);
                super.visitVarInsn(ALOAD, 2);
                super.visitMethodInsn(INVOKESPECIAL, QUARKUS_RESOURCE_AND_CLASS_SCANNER_INTERNAL_CLASS_NAME,
                        CTOR_METHOD_NAME, "(Ljava/util/Collection;)V", false);
                super.visitVarInsn(ASTORE, 7);
                super.visitVarInsn(ALOAD, 0);
                super.visitFieldInsn(GETFIELD, FLYWAY_SCANNER_INTERNAL_CLASS_NAME, "resources", "Ljava/util/List;");
                super.visitVarInsn(ALOAD, 7);
                super.visitMethodInsn(INVOKEINTERFACE, FLYWAY_RESOURCE_AND_CLASS_SCANNER_INTERNAL_CLASS_NAME,
                        "scanForResources", "()Ljava/util/Collection;", true);
                super.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "addAll", "(Ljava/util/Collection;)Z", true);
                super.visitInsn(POP);
                super.visitVarInsn(ALOAD, 0);
                super.visitFieldInsn(GETFIELD, FLYWAY_SCANNER_INTERNAL_CLASS_NAME, "classes", "Ljava/util/List;");
                super.visitVarInsn(ALOAD, 7);
                super.visitMethodInsn(INVOKEINTERFACE, FLYWAY_RESOURCE_AND_CLASS_SCANNER_INTERNAL_CLASS_NAME,
                        "scanForClasses", "()Ljava/util/Collection;", true);
                super.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "addAll", "(Ljava/util/Collection;)Z", true);
                super.visitInsn(POP);
                super.visitInsn(RETURN);
                super.visitMaxs(3, 8);
            }
        }
    }
}
