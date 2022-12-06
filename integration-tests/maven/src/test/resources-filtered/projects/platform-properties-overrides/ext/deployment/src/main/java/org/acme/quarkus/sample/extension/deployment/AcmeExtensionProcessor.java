package org.acme.quarkus.sample.extension.deployment;

import jakarta.inject.Singleton;

import org.acme.quarkus.sample.extension.ConfigReport;
import org.acme.quarkus.sample.extension.ConfigReportRecorder;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;

class AcmeExtensionProcessor {

    private static final String FEATURE = "acme-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem syntheticBean(ConfigReportRecorder recorder, NativeConfig nativeConfig) {
       return SyntheticBeanBuildItem.configure(ConfigReport.class)
    		   .scope(Singleton.class)
    		   .runtimeValue(recorder.configReport(nativeConfig.builderImage))
    		   .done();
    }
}
