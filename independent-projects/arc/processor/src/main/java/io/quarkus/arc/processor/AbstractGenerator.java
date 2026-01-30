package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.Arc;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.creator.BlockCreator;

abstract class AbstractGenerator {

    static final String DEFAULT_PACKAGE = Arc.class.getPackage().getName() + ".generator";
    static final String UNDERSCORE = "_";
    static final String SYNTHETIC_SUFFIX = "Synthetic";

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
        return Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false)
                .withLambdasAsAnonymousClasses(true);
    }

    /**
     * Creates an optimized Set.of() call based on the number of elements.
     * Uses Set.of(), Set.of(e1), Set.of(e1, e2), or Set.of(Object[]) depending on element count
     * to avoid unnecessary array creation for small sets.
     * <p>
     * IMPORTANT: we need to make sure the provided elements don't contain duplicates.
     *
     * @param bc the block creator
     * @param elements the list of elements to include in the set
     * @return an expression representing the Set.of() call
     */
    static Expr createSetOf(BlockCreator bc, List<? extends Expr> elements) {
        return switch (elements.size()) {
            case 0 -> bc.invokeStatic(MethodDescs.SET_OF_0);
            case 1 -> bc.invokeStatic(MethodDescs.SET_OF_1, elements.get(0));
            case 2 -> bc.invokeStatic(MethodDescs.SET_OF_2, elements.get(0), elements.get(1));
            default -> bc.invokeStatic(MethodDescs.SET_OF_VARARGS, bc.newArray(Object.class, elements));
        };
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
