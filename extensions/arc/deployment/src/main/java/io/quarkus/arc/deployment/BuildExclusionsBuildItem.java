package io.quarkus.arc.deployment;

import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A type of build item that contains only declaring classes, methods and fields that have been annotated with
 * unsuccessful build time conditions. It aims to be used to manage the exclusion of the annotations thanks to the
 * build time conditions also known as {@code IfBuildProfile}, {@code UnlessBuildProfile}, {@code IfBuildProperty} and
 * {@code UnlessBuildProperty}
 *
 * @see io.quarkus.arc.deployment.PreAdditionalBeanBuildTimeConditionBuildItem
 * @see io.quarkus.arc.profile.IfBuildProfile
 * @see io.quarkus.arc.profile.UnlessBuildProfile
 * @see io.quarkus.arc.properties.IfBuildProperty
 * @see io.quarkus.arc.properties.UnlessBuildProperty
 */
public final class BuildExclusionsBuildItem extends SimpleBuildItem {

    private final Set<String> excludedDeclaringClasses;
    private final Set<String> excludedMethods;
    private final Set<String> excludedFields;

    public BuildExclusionsBuildItem(Set<String> excludedDeclaringClasses,
            Set<String> excludedMethods,
            Set<String> excludedFields) {
        this.excludedDeclaringClasses = excludedDeclaringClasses;
        this.excludedMethods = excludedMethods;
        this.excludedFields = excludedFields;
    }

    public Set<String> getExcludedDeclaringClasses() {
        return excludedDeclaringClasses;
    }

    public Set<String> getExcludedMethods() {
        return excludedMethods;
    }

    public Set<String> getExcludedFields() {
        return excludedFields;
    }

    /**
     * Indicates whether the given target is excluded following the next rules:
     * <p>
     * <ul>
     * <li>In case of a class it will check if it is part of the excluded classes</li>
     * <li>In case of a method it will check if it is part of the excluded methods and if its declaring class
     * is excluded</li>
     * <li>In case of a method parameter it will check if its corresponding method is part of the excluded methods
     * and if its declaring class is excluded</li>
     * <li>In case of a field it will check if it is part of the excluded field and if its declaring class is excluded</li>
     * <li>In all other cases, it is not excluded</li>
     * </ul>
     * 
     * @param target the target to check.
     * @return {@code true} if the target is excluded, {@code false} otherwise.
     */
    public boolean isExcluded(AnnotationTarget target) {
        switch (target.kind()) {
            case CLASS:
                return excludedDeclaringClasses.contains(targetMapper(target));
            case METHOD:
                return excludedMethods.contains(targetMapper(target)) ||
                        excludedDeclaringClasses.contains(targetMapper(target.asMethod().declaringClass()));
            case METHOD_PARAMETER:
                final MethodInfo method = target.asMethodParameter().method();
                return excludedMethods.contains(targetMapper(method)) ||
                        excludedDeclaringClasses.contains(targetMapper(method.declaringClass()));
            case FIELD:
                return excludedFields.contains(targetMapper(target)) ||
                        excludedDeclaringClasses.contains(targetMapper(target.asField().declaringClass()));
            default:
                return false;
        }
    }

    /**
     * Converts the given target into a String unique representation.
     * 
     * @param target the target to convert.
     * @return a unique representation as a {@code String} of the target
     */
    public static String targetMapper(AnnotationTarget target) {
        final AnnotationTarget.Kind kind = target.kind();
        if (kind == AnnotationTarget.Kind.CLASS) {
            return target.asClass().toString();
        } else if (kind == AnnotationTarget.Kind.METHOD) {
            final MethodInfo method = target.asMethod();
            return String.format("%s#%s", method.declaringClass(), method);
        }
        return target.asField().toString();
    }
}
