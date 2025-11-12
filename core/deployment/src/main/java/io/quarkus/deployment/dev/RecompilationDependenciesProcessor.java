package io.quarkus.deployment.dev;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

public class RecompilationDependenciesProcessor {

    /**
     * Consolidation step, which prepares recompilation of dependent classes.
     * <p/>
     * Resolves inner classes to their outer classes. Inner classes and outer classes share
     * source file (at least in java). For our goal of recompilation we therefore only need to outer class.
     * <p/>
     * After resolving outer classes, all the mappings of referenced type to annotated class are combined into one mapping,
     * deduplicated by class name, and
     * then given to the {@link RuntimeUpdatesProcessor#setRecompilationDependencies(Map)}.
     *
     * @param combinedIndexBuildItem Index, only the precomputed index is used, since for the purpose of (re)compilation the
     *        class has to be in an application source path - which are always indexed.
     * @param recompilationDependenciesBuildItems Provides the dependencies between classes. Currently, produced only by
     *        extensions.
     */
    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    public void consolidateRecompilationDependencies(CombinedIndexBuildItem combinedIndexBuildItem,
            List<RecompilationDependenciesBuildItem> recompilationDependenciesBuildItems) {

        // Cleanup and combine all the dependencyToAnnotatedClasses maps:
        // - Resolve inner classes to their top-level class names
        // - Remove entries where the class is not in the index

        Map<DotName, Set<DotName>> dependencyToAnnotatedClasses = new HashMap<>();
        for (RecompilationDependenciesBuildItem buildItem : recompilationDependenciesBuildItems) {
            for (Map.Entry<DotName, Set<DotName>> entry : buildItem.getClassToRecompilationTargets().entrySet()) {

                DotName dependency = resolveOutermostClassName(entry.getKey(), combinedIndexBuildItem.getIndex());
                if (dependency == null) {
                    continue;
                }

                for (DotName annotatedClass : entry.getValue()) {
                    annotatedClass = resolveOutermostClassName(annotatedClass, combinedIndexBuildItem.getIndex());
                    if (annotatedClass == null) {
                        continue;
                    }

                    dependencyToAnnotatedClasses.computeIfAbsent(dependency, k -> new HashSet<>()).add(annotatedClass);
                }
            }
        }

        if (RuntimeUpdatesProcessor.INSTANCE != null) {
            RuntimeUpdatesProcessor.INSTANCE.setRecompilationDependencies(dependencyToAnnotatedClasses);
        }
    }

    private DotName resolveOutermostClassName(DotName name, IndexView index) {

        ClassInfo classInfo = index.getClassByName(name);
        if (classInfo == null) {
            return null;
        }

        if (classInfo.nestingType() != ClassInfo.NestingType.TOP_LEVEL) {
            return resolveOutermostClassName(classInfo.enclosingClassAlways(), index);
        }

        return name;
    }
}
