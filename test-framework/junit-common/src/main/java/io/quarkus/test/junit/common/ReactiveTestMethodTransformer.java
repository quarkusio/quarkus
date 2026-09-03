package io.quarkus.test.junit.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;

/**
 * Transforms test classes so that {@code @Test @TestTransaction} methods returning
 * {@code Uni} are discoverable by JUnit 5. JUnit rejects non-void {@code @Test} methods,
 * so for each such method this transformer:
 * <ol>
 * <li>Removes the {@code @Test} annotation from the original Uni-returning method</li>
 * <li>Adds a synthetic void stub method (original name + {@value #REACTIVE_TEST_SUFFIX})
 * with {@code @Test} and {@code @TestTransaction} annotations</li>
 * </ol>
 * JUnit discovers the synthetic stub; at execution time the framework strips the suffix
 * and invokes the original Uni-returning method.
 */
public final class ReactiveTestMethodTransformer {

    public static final String REACTIVE_TEST_SUFFIX = "$quarkusRxTest";

    private static final String TEST_ANNOTATION_FQN = "org.junit.jupiter.api.Test";
    private static final String TEST_TRANSACTION_FQN = "io.quarkus.test.TestTransaction";
    private static final String ORDER_ANNOTATION_FQN = "org.junit.jupiter.api.Order";

    private static final DotName TEST_ANNOTATION = DotName.createSimple(TEST_ANNOTATION_FQN);
    private static final DotName TEST_TRANSACTION_ANNOTATION = DotName.createSimple(TEST_TRANSACTION_FQN);
    private static final DotName ORDER_ANNOTATION = DotName.createSimple(ORDER_ANNOTATION_FQN);
    private static final DotName UNI_DOTNAME = DotName.createSimple("io.smallrye.mutiny.Uni");

    private static final String TEST_ANNOTATION_DESC = "L" + TEST_ANNOTATION_FQN.replace('.', '/') + ";";
    private static final String UNI_DESCRIPTOR_SUFFIX = "Lio/smallrye/mutiny/Uni;";

    // Lio/quarkus/test/TestTransaction; as raw bytes for fast constant pool scanning
    private static final byte[] TEST_TRANSACTION_BYTES = ("L" + TEST_TRANSACTION_FQN.replace('.', '/') + ";").getBytes();

    private ReactiveTestMethodTransformer() {
    }

    /**
     * Strips the reactive test suffix from a method name, if present.
     */
    public static String stripReactiveTestSuffix(String methodName) {
        if (methodName.endsWith(REACTIVE_TEST_SUFFIX)) {
            return methodName.substring(0, methodName.length() - REACTIVE_TEST_SUFFIX.length());
        }
        return methodName;
    }

    /**
     * Transforms the class if it contains {@code @Test @TestTransaction} methods returning
     * {@code Uni}. Returns {@code null} if no transformation is needed.
     */
    public static byte[] transformIfNeeded(byte[] classBytes, ClassLoader classLoader) {
        List<ReactiveTestMethod> methods = findReactiveTestMethods(classBytes);
        if (methods.isEmpty()) {
            return null;
        }
        return transform(classBytes, methods, classLoader);
    }

    /**
     * Uses Jandex to scan class bytes for methods annotated with both {@code @Test} and
     * {@code @TestTransaction} that return {@code Uni}.
     */
    private static List<ReactiveTestMethod> findReactiveTestMethods(byte[] classBytes) {
        if (!containsBytes(classBytes, TEST_TRANSACTION_BYTES)) {
            return List.of();
        }

        Indexer indexer = new Indexer();
        try {
            indexer.index(new ByteArrayInputStream(classBytes));
        } catch (IOException e) {
            return List.of();
        }
        Index index = indexer.complete();
        if (index.getKnownClasses().isEmpty()) {
            return List.of();
        }
        ClassInfo classInfo = index.getKnownClasses().iterator().next();

        List<ReactiveTestMethod> methods = new ArrayList<>();
        for (MethodInfo method : classInfo.methods()) {
            if (method.returnType().name().equals(UNI_DOTNAME)
                    && method.hasAnnotation(TEST_ANNOTATION)
                    && method.hasAnnotation(TEST_TRANSACTION_ANNOTATION)) {
                int orderValue = -1;
                AnnotationInstance orderAnn = method.annotation(ORDER_ANNOTATION);
                if (orderAnn != null && orderAnn.value() != null) {
                    orderValue = orderAnn.value().asInt();
                }
                String[] paramTypes = method.parameterTypes().stream()
                        .map(t -> t.name().toString())
                        .toArray(String[]::new);
                methods.add(new ReactiveTestMethod(method.name(), paramTypes, method.flags(), orderValue));
            }
        }
        return methods;
    }

    /**
     * Transforms the class by removing {@code @Test} from the original Uni-returning
     * methods (using raw ASM) and adding synthetic void stub methods that JUnit can
     * discover (using Gizmo {@link ClassTransformer}).
     */
    private static byte[] transform(byte[] classBytes, List<ReactiveTestMethod> methods, ClassLoader classLoader) {
        ClassReader reader = new ClassReader(classBytes);
        String className = reader.getClassName().replace('/', '.');

        // Gizmo ClassTransformer adds synthetic void stub methods
        ClassTransformer transformer = new ClassTransformer(className);
        for (ReactiveTestMethod method : methods) {
            MethodCreator mc = transformer.addMethod(
                    method.name + REACTIVE_TEST_SUFFIX, void.class, (Object[]) method.parameterTypes);
            mc.setModifiers(method.access);
            mc.addAnnotation(TEST_ANNOTATION_FQN, RetentionPolicy.RUNTIME);
            mc.addAnnotation(TEST_TRANSACTION_FQN, RetentionPolicy.RUNTIME);
            if (method.orderValue >= 0) {
                mc.addAnnotation(ORDER_ANNOTATION_FQN, RetentionPolicy.RUNTIME)
                        .addValue("value", method.orderValue);
            }
            mc.returnVoid();
        }

        Set<String> methodNames = new HashSet<>();
        for (ReactiveTestMethod m : methods) {
            methodNames.add(m.name);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                ClassLoader cl = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
                try {
                    Class<?> c1 = Class.forName(type1.replace('/', '.'), false, cl);
                    Class<?> c2 = Class.forName(type2.replace('/', '.'), false, cl);
                    if (c1.isAssignableFrom(c2)) {
                        return type1;
                    }
                    if (c2.isAssignableFrom(c1)) {
                        return type2;
                    }
                    if (c1.isInterface() || c2.isInterface()) {
                        return "java/lang/Object";
                    }
                    do {
                        c1 = c1.getSuperclass();
                    } while (!c1.isAssignableFrom(c2));
                    return c1.getName().replace('.', '/');
                } catch (ClassNotFoundException e) {
                    return "java/lang/Object";
                }
            }
        };

        // Raw ASM visitor strips @Test from the original Uni-returning methods
        ClassVisitor strippingVisitor = new ClassVisitor(Gizmo.ASM_API_VERSION, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (methodNames.contains(name) && descriptor.endsWith(UNI_DESCRIPTOR_SUFFIX)) {
                    return new MethodVisitor(Gizmo.ASM_API_VERSION, mv) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (TEST_ANNOTATION_DESC.equals(desc)) {
                                return null;
                            }
                            return super.visitAnnotation(desc, visible);
                        }
                    };
                }
                return mv;
            }
        };

        // Chain: reader → gizmo (adds synthetic methods) → stripping (removes @Test) → writer
        ClassVisitor gizmoVisitor = transformer.applyTo(strippingVisitor);
        reader.accept(gizmoVisitor, 0);
        return writer.toByteArray();
    }

    /**
     * Reads class bytes from the given classloader's resources.
     */
    public static byte[] readClassBytes(String className, ClassLoader classLoader) {
        String resourceName = className.replace('.', '/') + ".class";
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            if (is == null) {
                return null;
            }
            return is.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        int limit = haystack.length - needle.length;
        outer: for (int i = 0; i <= limit; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static final class ReactiveTestMethod {
        final String name;
        final String[] parameterTypes;
        final int access;
        final int orderValue;

        ReactiveTestMethod(String name, String[] parameterTypes, int access, int orderValue) {
            this.name = name;
            this.parameterTypes = parameterTypes;
            this.access = access;
            this.orderValue = orderValue;
        }
    }
}
