package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeConditionalRootClassBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeDeclaredClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootClassBuildItem;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.AnnotationValue;
import io.smallrye.classfile.Attributes;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

/**
 * Collects tree-shake roots from classes that use GraalVM substitution annotations
 * ({@code @TargetClass}, {@code @Substitute}, {@code @Alias}, etc.).
 * <p>
 * These classes are discovered by {@code native-image} through classpath scanning, not through code references.
 * A substitution class is only needed if its target class is reachable, and {@code native-image} fails if a
 * substitution class is present while its target isn't. So a class annotated with {@code @TargetClass} is a root
 * conditional on its target, and its target is a root conditional on the substitution class. If the target is given as
 * {@code innerClass} of another class, {@code native-image} looks it up among the declared classes of the enclosing
 * classes, which must then be loadable. Classes whose target can't be determined statically
 * ({@code classNameProvider}) and other classes using these annotations are unconditional roots.
 */
final class SubstitutionRootCollector {

    private static final Logger log = Logger.getLogger(SubstitutionRootCollector.class);

    private static final byte[] SVM_ANNOTATE_MARKER = "com/oracle/svm/core/annotate/".getBytes(StandardCharsets.UTF_8);
    private static final String TARGET_CLASS_DESCRIPTOR = "Lcom/oracle/svm/core/annotate/TargetClass;";
    private static final String NO_CLASS_NAME_PROVIDER_DESCRIPTOR = "Lcom/oracle/svm/core/annotate/TargetClass$NoClassNameProvider;";

    private SubstitutionRootCollector() {
    }

    /**
     * Collects roots from the substitution classes found in the given tree.
     *
     * @param tree content tree to scan
     * @param roots receives the unconditional roots
     * @param conditionalRoots receives the conditional roots
     * @param declaredClasses receives the classes whose declared classes {@code native-image} looks up
     *        to resolve {@code innerClass} targets
     */
    static void collect(PathTree tree, BuildProducer<JarTreeShakeRootClassBuildItem> roots,
            BuildProducer<JarTreeShakeConditionalRootClassBuildItem> conditionalRoots,
            BuildProducer<JarTreeShakeDeclaredClassesBuildItem> declaredClasses) {
        try (OpenPathTree openTree = tree.open()) {
            openTree.walk(visit -> {
                String path = visit.getResourceName();
                if (path.endsWith(".class") && !path.endsWith("module-info.class")) {
                    try {
                        byte[] classBytes = Files.readAllBytes(visit.getPath());
                        if (containsBytes(classBytes, SVM_ANNOTATE_MARKER)) {
                            addRoot(path, classBytes, roots, conditionalRoots, declaredClasses);
                        }
                    } catch (IOException e) {
                        log.debugf(e, "Failed to read class file: %s", path);
                    }
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read content of " + tree.getRoots(), e);
        }
    }

    private static void addRoot(String path, byte[] classBytes, BuildProducer<JarTreeShakeRootClassBuildItem> roots,
            BuildProducer<JarTreeShakeConditionalRootClassBuildItem> conditionalRoots,
            BuildProducer<JarTreeShakeDeclaredClassesBuildItem> declaredClasses) {
        final String className;
        final List<String> targetChain;
        try {
            final ClassModel classModel = ClassFile.of().parse(classBytes);
            className = classModel.thisClass().asInternalName().replace('/', '.');
            targetChain = getTargetClassChain(classModel);
        } catch (IllegalArgumentException | ClassCastException e) {
            // can't tell what it is, keep it like any other class using the annotations
            log.debugf(e, "Failed to read the GraalVM annotations of class file: %s", path);
            roots.produce(
                    new JarTreeShakeRootClassBuildItem(path.substring(0, path.length() - ".class".length()).replace('/', '.')));
            return;
        }
        if (targetChain == null) {
            roots.produce(new JarTreeShakeRootClassBuildItem(className));
            return;
        }
        final int targetIndex = targetChain.size() - 1;
        final String target = targetChain.get(targetIndex);
        conditionalRoots.produce(new JarTreeShakeConditionalRootClassBuildItem(target, className));
        // a substitution reachable through code references still requires its target
        conditionalRoots.produce(new JarTreeShakeConditionalRootClassBuildItem(className, target));
        // native-image resolves each innerClass element in the declared classes of the previous one
        for (int i = 0; i < targetIndex; i++) {
            declaredClasses.produce(new JarTreeShakeDeclaredClassesBuildItem(className, targetChain.get(i)));
        }
    }

    /**
     * Returns the dot-separated names of the {@code @TargetClass} target, preceded by the classes enclosing it
     * as given by {@code innerClass}, or {@code null} if the class has no {@code @TargetClass} annotation
     * or its target can't be determined statically.
     */
    private static List<String> getTargetClassChain(ClassModel classModel) {
        final Optional<RuntimeVisibleAnnotationsAttribute> annotations = classModel
                .findAttribute(Attributes.runtimeVisibleAnnotations());
        if (annotations.isEmpty()) {
            return null;
        }
        for (Annotation annotation : annotations.get().annotations()) {
            if (annotation.className().equalsString(TARGET_CLASS_DESCRIPTOR)) {
                return getTargetClassChain(annotation);
            }
        }
        return null;
    }

    private static List<String> getTargetClassChain(Annotation targetClass) {
        String target = null;
        AnnotationValue.OfArray innerClasses = null;
        for (AnnotationElement element : targetClass.elements()) {
            final AnnotationValue value = element.value();
            if (element.name().equalsString("value")) {
                String descriptor = ((AnnotationValue.OfClass) value).className().stringValue();
                if (!descriptor.equals(TARGET_CLASS_DESCRIPTOR)) {
                    target = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                }
            } else if (element.name().equalsString("className")) {
                String className = ((AnnotationValue.OfString) value).stringValue();
                if (target == null && !className.isEmpty()) {
                    target = className;
                }
            } else if (element.name().equalsString("innerClass")) {
                innerClasses = (AnnotationValue.OfArray) value;
            } else if (element.name().equalsString("classNameProvider")
                    && !((AnnotationValue.OfClass) value).className().equalsString(NO_CLASS_NAME_PROVIDER_DESCRIPTOR)) {
                return null;
            }
        }
        if (target == null) {
            return null;
        }
        if (innerClasses == null || innerClasses.values().isEmpty()) {
            return List.of(target);
        }
        final List<String> chain = new ArrayList<>(innerClasses.values().size() + 1);
        chain.add(target);
        for (AnnotationValue innerClass : innerClasses.values()) {
            target = target + '$' + ((AnnotationValue.OfString) innerClass).stringValue();
            chain.add(target);
        }
        return chain;
    }

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
