package io.quarkus.arc.processor;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.Arc;
import io.quarkus.gizmo2.ClassHierarchyLocator;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Gizmo;

abstract class AbstractGenerator {

    static final String DEFAULT_PACKAGE = Arc.class.getPackage().getName() + ".generator";
    static final String UNDERSCORE = "_";
    static final String SYNTHETIC_SUFFIX = "Synthetic";

    private static final Map<ClassDesc, ClassHierarchyLocator.Result> classHierarchyCache = new ConcurrentHashMap<>();

    protected final boolean generateSources;
    protected final ReflectionRegistration reflectionRegistration;

    public AbstractGenerator(boolean generateSources, ReflectionRegistration reflectionRegistration) {
        this.generateSources = generateSources;
        this.reflectionRegistration = reflectionRegistration;
    }

    public AbstractGenerator(boolean generateSources) {
        this(generateSources, null);
    }

    static Gizmo gizmo(ClassOutput classOutput) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl == null) {
            throw new IllegalStateException("No TCCL available");
        }
        ClassHierarchyLocator locator = ClassHierarchyLocator.forClassParsing(tccl).cached(() -> classHierarchyCache);
        return Gizmo.create(classOutput)
                .withClassHierarchyLocator(locator)
                .withDebugInfo(false)
                .withParameters(false)
                .withLambdasAsAnonymousClasses(true);
    }

    /**
     * Generates a class name from a target package, base name and suffix. When the class
     * is located in a default package, the target package name is an empty string.
     *
     * @param targetPackage name of the target package
     * @param baseName simple class name
     * @param suffix suffix to append to the generated name
     * @return generated name
     */
    static String generatedNameFromTarget(String targetPackage, String baseName, String suffix) {
        if (targetPackage == null || targetPackage.isEmpty()) {
            return baseName + suffix;
        } else {
            return targetPackage + "." + baseName + suffix;
        }
    }

    /**
     * {@return a simple name of the given {@code beanClassName}, stripped of the {@link BeanGenerator#BEAN_SUFFIX}}
     */
    protected final String getBeanBaseName(String beanClassName) {
        String simpleName = beanClassName.contains(".")
                ? beanClassName.substring(beanClassName.lastIndexOf(".") + 1)
                : beanClassName;
        return simpleName.substring(0, simpleName.lastIndexOf(BeanGenerator.BEAN_SUFFIX));
    }

    protected final boolean isReflectionFallbackNeeded(MethodInfo method, String targetPackage) {
        if (Modifier.isPublic(method.flags())) {
            return false;
        }
        // Reflection fallback is needed for:
        // 1. private methods
        if (Modifier.isPrivate(method.flags())) {
            return true;
        }
        // 2. non-public methods declared on superclasses located in a different package
        return !DotNames.packagePrefix(method.declaringClass().name()).equals(targetPackage);
    }

    protected final boolean isReflectionFallbackNeeded(FieldInfo field, String targetPackage, BeanInfo bean) {
        if (Modifier.isPublic(field.flags())) {
            return false;
        }
        // Reflection fallback is needed for:
        // 1. private fields if the transformation is turned off OR if the field's declaring class != bean class
        if (Modifier.isPrivate(field.flags())) {
            if (!bean.getDeployment().transformPrivateInjectedFields
                    || !field.declaringClass().name().equals(bean.getBeanClass())) {
                return true;
            }
        }
        // 2. for non-public fields declared on superclasses located in a different package
        return !DotNames.packagePrefix(field.declaringClass().name()).equals(targetPackage);
    }

}
