package io.quarkus.deployment.steps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.SetClassPathSystemPropBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.ClassPathSystemPropertyRecorder;

public class ClassPathSystemPropBuildStep {

    @BuildStep
    public void produce(BuildProducer<SetClassPathSystemPropBuildItem> producer, CurateOutcomeBuildItem curateOutcome) {
        boolean truffleUsed = curateOutcome.getApplicationModel().getDependencies().stream()
                .anyMatch(d -> d.getGroupId().equals("org.graalvm.polyglot"));
        if (truffleUsed) {
            producer.produce(new SetClassPathSystemPropBuildItem());
        }
    }

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
        String classPathValue = Stream.concat(parentFirst.stream(), regular.stream()).map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.joining(":"));
        recorder.set(classPathValue);
    }
}
