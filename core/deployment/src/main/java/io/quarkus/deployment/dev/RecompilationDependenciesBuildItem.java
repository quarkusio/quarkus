package io.quarkus.deployment.dev;

import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Defines the relationship between classes for the purposes of recompilation.
 * <p/>
 * Idea is that multiple classes can be dependent on one other class. For this BuildItem a class can be an outermost class, a
 * nested class, an annotation, an interface, an enum, or basically anything that has an FQN.
 * A consolidation step in {@link RecompilationDependenciesProcessor} cleans up the collected recompilation dependencies and
 * removes duplicates.
 * <p/>
 * The collected recompilation dependencies are used to build a relationship tree between the classes in the
 * {@link RuntimeUpdatesProcessor}, which is build and traversed from the root up and outwards. Class a -> Set of Class -> where
 * each element class can resolve to another set of classes.
 * <p/>
 * The collected recompilation dependencies are used to determine the set of classes which need to additionally be recompiled
 * when one other class changes.
 * <p/>
 * For performance reasons, the scope of the collected recompilation dependencies should be quite narrow. For an
 * example, the quarkus-mapstruct extension uses this BuildItem to recompile the dependent Mapper classes, once any of the
 * referenced model classes
 * changes.
 */
public final class RecompilationDependenciesBuildItem extends MultiBuildItem {

    private final Map<DotName, Set<DotName>> classToRecompilationTargets;

    public RecompilationDependenciesBuildItem(Map<DotName, Set<DotName>> classToRecompilationTargets) {
        this.classToRecompilationTargets = classToRecompilationTargets;
    }

    /**
     * Returns the map of recompilation dependencies.
     * <p>
     * The key is a dependency class name, while the value is a set of dependent class names that need to be recompiled as well
     * when the
     * dependency class changes.
     *
     * @return a map where each key is a dependency class name and each value is a set of class names that depend on it
     */
    public Map<DotName, Set<DotName>> getClassToRecompilationTargets() {
        return classToRecompilationTargets;
    }
}
