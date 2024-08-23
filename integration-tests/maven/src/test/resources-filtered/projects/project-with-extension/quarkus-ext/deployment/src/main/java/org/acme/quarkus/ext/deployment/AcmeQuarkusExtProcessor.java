package org.acme.quarkus.ext.deployment;

import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.Dependency;

import org.acme.AcmeQuarkusExtRecorder;
import org.acme.ModuleList;

class AcmeQuarkusExtProcessor {

    private static final String FEATURE = "acme-quarkus-ext";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem syntheticBean(AcmeQuarkusExtRecorder recorder, CurateOutcomeBuildItem curateOutcome) {
        var localModules = curateOutcome.getApplicationModel().getDependencies().stream()
                .filter(Dependency::isWorkspaceModule)
                .map(Dependency::toCompactCoords)
                .sorted()
                .collect(Collectors.toList());
       return SyntheticBeanBuildItem.configure(ModuleList.class)
                .scope(Singleton.class)
                .runtimeValue(recorder.initLocalModules(localModules)) 
                .done();
    }
}
