package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows declaring classes and/or packages which should be considered compatible despite potentially using
 * {@link jakarta.enterprise.inject.Specializes} annotation.
 * <p>
 * Quarkus does not support {@link jakarta.enterprise.inject.Specializes} and will detect usage of this annotation
 * and throw a build time error for any occurrence that is not registered through this build item.
 */
public final class KnownSpecializesAnnotationOccurrenceBuildItem extends MultiBuildItem {

    private final Collection<DotName> classNames;
    private final Collection<String> packageNames;

    private KnownSpecializesAnnotationOccurrenceBuildItem(Collection<DotName> classNames, Collection<String> packageNames) {
        this.classNames = classNames;
        this.packageNames = packageNames;
    }

    public Collection<DotName> getClassNames() {
        return classNames;
    }

    public Collection<String> getPackageInfos() {
        return packageNames;
    }

    public static KnownSpecializesAnnotationOccurrenceBuildItem forClass(DotName className) {
        return new KnownSpecializesAnnotationOccurrenceBuildItem(Set.of(className), Collections.EMPTY_SET);
    }

    public static KnownSpecializesAnnotationOccurrenceBuildItem forClasses(Collection<DotName> classNames) {
        return new KnownSpecializesAnnotationOccurrenceBuildItem(classNames, Collections.EMPTY_SET);
    }

    public static KnownSpecializesAnnotationOccurrenceBuildItem forPackage(String packageName) {
        return new KnownSpecializesAnnotationOccurrenceBuildItem(Collections.EMPTY_SET, Set.of(packageName));
    }

    public static KnownSpecializesAnnotationOccurrenceBuildItem forPackages(Collection<String> packageName) {
        return new KnownSpecializesAnnotationOccurrenceBuildItem(Collections.EMPTY_SET, packageName);
    }
}
