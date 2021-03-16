package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds information about all known custom scopes in the deployment and has utility methods allowing to check
 * whether given class has some scope annotation.
 */
public final class CustomScopeAnnotationsBuildItem extends SimpleBuildItem {

    private final Set<DotName> customScopeNames;

    CustomScopeAnnotationsBuildItem(Set<DotName> customScopeNames) {
        this.customScopeNames = customScopeNames;
    }

    /**
     * Returns a collection of all known custom scopes represented as {@link DotName}.
     *
     * @return collection of known custom scopes (built-in scopes are not included)
     */
    public Collection<DotName> getCustomScopeNames() {
        return customScopeNames;
    }

    /**
     * Returns true if the given class has some of the custom scope annotations, false otherwise.
     * List of known custom scopes can be seen via {@link CustomScopeAnnotationsBuildItem#getCustomScopeNames()}.
     * In order to check for presence of any scope annotation (including built-in ones),
     * see {@link CustomScopeAnnotationsBuildItem#isScopeDeclaredOn(ClassInfo)}.
     *
     * @param clazz Class to check for annotations
     * @return true if the clazz contains some of the custom scope annotations, false otherwise
     */
    public boolean isCustomScopeDeclaredOn(ClassInfo clazz) {
        for (DotName scope : customScopeNames) {
            if (clazz.classAnnotation(scope) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param annotations
     * @return {@code true} if the collection contains a custom scope annotation, {@code false} otherwise
     */
    public boolean isCustomScopeIn(Collection<AnnotationInstance> annotations) {
        for (AnnotationInstance annotationInstance : annotations) {
            if (customScopeNames.contains(annotationInstance.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given class has some scope annotations, false otherwise.
     * This method check for all scope annotations, including built-in ones as well as custom scopes.
     * List of known custom scopes can be seen via {@link CustomScopeAnnotationsBuildItem#getCustomScopeNames()}.
     *
     * @param clazz Class to check for annotations
     * @return true if the clazz contains any scope annotation, false otherwise
     */
    public boolean isScopeDeclaredOn(ClassInfo clazz) {
        return BuiltinScope.isDeclaredOn(clazz) || isCustomScopeDeclaredOn(clazz);
    }

    /**
     * 
     * @param annotations
     * @return {@code true} if the collection contains any scope annotation, {@code false} otherwise
     * @see #isCustomScopeIn(Collection)
     */
    public boolean isScopeIn(Collection<AnnotationInstance> annotations) {
        return !annotations.isEmpty() && (BuiltinScope.isIn(annotations) || isCustomScopeIn(annotations));
    }
}
