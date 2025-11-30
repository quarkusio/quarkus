package io.quarkus.deployment.dev;

import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Defines the relationship between classes for the purposes of recompilation.
 * <p/>
 * Idea is that multiple classes can be dependent on one other class. For this BuilItem a class can be, an outermost class, a
 * nested class, an annotation, an interface, an enum, or basically anything that has an FQDN.
 * A consolidation step in {@link RecompilationDependenciesProcessor} cleans up the collected recompilation dependencies and
 * removes duplicates.
 * <p/>
 * The collected recompilation dependencies are used to build a relationship tree between the classes in the
 * {@link RuntimeUpdatesProcessor}, which is build and traversed from the root up and outwards. Class a -> Set of Class -> where
 * each element class can resolve to another set of classes.
 * <p/>
 * In the end, the collected recompilation dependencies will be used to determine, what set of classes need to recompiled in
 * addition, when one class changes.
 * <p/>
 * For performance reasons however, the scope of the collected recompilation dependencies should be quite narrow. For an
 * example, the quarkus-mapstruct extension uses this BuildItem to recompile the dependent Mapper classes, once any of the
 * referenced model classes
 * changes.
 */
public final class RecompilationDependenciesBuildItem extends MultiBuildItem {

    private final Map<DotName, Set<DotName>> classToRecompilationTargets;

    public RecompilationDependenciesBuildItem(Map<DotName, Set<DotName>> classToRecompilationTargets) {
        this.classToRecompilationTargets = classToRecompilationTargets;
    }

    public Map<DotName, Set<DotName>> getClassToRecompilationTargets() {
        return classToRecompilationTargets;
    }
}
