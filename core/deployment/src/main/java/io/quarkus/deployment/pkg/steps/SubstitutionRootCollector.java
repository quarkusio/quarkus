package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeConditionalRootClassBuildItem;
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
 * conditional on its target, and its target is a root conditional on the substitution class. Classes whose target can't be
 * determined statically ({@code classNameProvider})
 * and other classes using these annotations are unconditional roots.
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
     */
    static void collect(PathTree tree, BuildProducer<JarTreeShakeRootClassBuildItem> roots,
            BuildProducer<JarTreeShakeConditionalRootClassBuildItem> conditionalRoots) {
        try (OpenPathTree openTree = tree.open()) {
            openTree.walk(visit -> {
                String path = visit.getResourceName();
                if (path.endsWith(".class") && !path.endsWith("module-info.class")) {
                    try {
                        byte[] classBytes = Files.readAllBytes(visit.getPath());
                        if (containsBytes(classBytes, SVM_ANNOTATE_MARKER)) {
                            addRoot(path, classBytes, roots, conditionalRoots);
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
            BuildProducer<JarTreeShakeConditionalRootClassBuildItem> conditionalRoots) {
        final String className;
        final String target;
        try {
            final ClassModel classModel = ClassFile.of().parse(classBytes);
            className = classModel.thisClass().asInternalName().replace('/', '.');
            target = getTargetClassName(classModel);
        } catch (IllegalArgumentException | ClassCastException e) {
            // can't tell what it is, keep it like any other class using the annotations
            log.debugf(e, "Failed to read the GraalVM annotations of class file: %s", path);
            roots.produce(
                    new JarTreeShakeRootClassBuildItem(path.substring(0, path.length() - ".class".length()).replace('/', '.')));
            return;
        }
        if (target == null) {
            roots.produce(new JarTreeShakeRootClassBuildItem(className));
        } else {
            conditionalRoots.produce(new JarTreeShakeConditionalRootClassBuildItem(target, className));
            // a substitution reachable through code references still requires its target
            conditionalRoots.produce(new JarTreeShakeConditionalRootClassBuildItem(className, target));
        }
    }

    /**
     * Returns the dot-separated name of the {@code @TargetClass} target,
     * or {@code null} if the class has no {@code @TargetClass} annotation or its target can't be determined statically.
     */
    private static String getTargetClassName(ClassModel classModel) {
        final Optional<RuntimeVisibleAnnotationsAttribute> annotations = classModel
                .findAttribute(Attributes.runtimeVisibleAnnotations());
        if (annotations.isEmpty()) {
            return null;
        }
        for (Annotation annotation : annotations.get().annotations()) {
            if (annotation.className().equalsString(TARGET_CLASS_DESCRIPTOR)) {
                return getTargetClassName(annotation);
            }
        }
        return null;
    }

    private static String getTargetClassName(Annotation targetClass) {
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
        if (target == null || innerClasses == null || innerClasses.values().isEmpty()) {
            return target;
        }
        final StringBuilder sb = new StringBuilder(target);
        for (AnnotationValue innerClass : innerClasses.values()) {
            sb.append('$').append(((AnnotationValue.OfString) innerClass).stringValue());
        }
        return sb.toString();
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
