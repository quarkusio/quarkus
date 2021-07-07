package io.quarkus.extest.deployment;

import java.io.IOException;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.extest.runtime.classpath.ClasspathEntriesRecorder;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries.Phase;
import io.quarkus.extest.runtime.config.TestBuildTimeConfig;

public class ClasspathEntryRecordingBuildStep {
    /**
     * Register the CDI beans that are needed by the test extension
     *
     * @param additionalBeans - producer for additional bean items
     */
    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(RecordedClasspathEntries.class)
                .setUnremovable()
                .build());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordDuringAugmentation(ClasspathEntriesRecorder classpathEntriesRecorder, TestBuildTimeConfig config)
            throws IOException {
        classpathEntriesRecorder.record(Phase.AUGMENTATION,
                ClasspathEntriesRecorder.gather(config.classpathEntriesToRecord));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordDuringStaticInit(ClasspathEntriesRecorder classpathEntriesRecorder, TestBuildTimeConfig config)
            throws IOException {
        classpathEntriesRecorder.gatherAndRecord(Phase.STATIC_INIT, config.classpathEntriesToRecord);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void recordDuringRuntimeInit(ClasspathEntriesRecorder classpathEntriesRecorder, TestBuildTimeConfig config)
            throws IOException {
        classpathEntriesRecorder.gatherAndRecord(Phase.RUNTIME_INIT, config.classpathEntriesToRecord);
    }

}
