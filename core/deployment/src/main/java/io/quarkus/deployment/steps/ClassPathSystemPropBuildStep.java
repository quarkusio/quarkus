package io.quarkus.deployment.steps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.SetClassPathSystemPropBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.ClassPathSystemPropertyRecorder;

@SuppressWarnings("removal")
public class ClassPathSystemPropBuildStep {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void set(List<SetClassPathSystemPropBuildItem> setCPItems,
            CurateOutcomeBuildItem curateOutcome,
            ClassPathSystemPropertyRecorder recorder) {
        if (setCPItems.isEmpty()) {
            return;
        }
        Collection<ResolvedDependency> runtimeDependencies = curateOutcome.getApplicationModel().getRuntimeDependencies();
        List<Path> parentFirst = new ArrayList<>();
        List<Path> regular = new ArrayList<>();
        for (ResolvedDependency dependency : runtimeDependencies) {
            if (dependency.isClassLoaderParentFirst()) {
                parentFirst.addAll(dependency.getContentTree().getRoots());
            } else {
                regular.addAll(dependency.getContentTree().getRoots());

            }
        }
        List<String> allJarPaths = Stream.concat(parentFirst.stream(), regular.stream()).map(p -> p.toAbsolutePath().toString())
                .toList();
        recorder.set(allJarPaths);
    }
}
