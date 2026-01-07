package io.quarkus.extest.deployment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.extest.runtime.classpath.ClasspathEntriesRecorder;
import io.quarkus.extest.runtime.classpath.ClasspathRecordingConfig;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries.Phase;

public class ClasspathEntryRecordingBuildStep {
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerRecordedClasspathEntries(
            ClasspathEntriesRecorder classpathEntriesRecorder,
            ClasspathRecordingConfig config,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        Optional<Path> recordFilePath = config.recordFile();
        if (recordFilePath.isEmpty()) {
            return;
        }
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(RecordedClasspathEntries.class)
                .runtimeValue(classpathEntriesRecorder.recordedClasspathEntries(recordFilePath.get().toString()))
                .done());
    }

    @BuildStep
    // This makes sure we execute this step even though it doesn't produce anything useful for the build
    // (just side-effects).
    @Produce(FeatureBuildItem.class)
    void recordDuringAugmentation(ClasspathRecordingConfig config)
            throws IOException {
        List<String> resourcesToRecord = getResourcesToRecord(config);
        if (resourcesToRecord.isEmpty()) {
            return;
        }
        ClasspathEntriesRecorder.record(getRecordFilePath(config), Phase.AUGMENTATION,
                ClasspathEntriesRecorder.gather(resourcesToRecord));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordDuringStaticInit(ClasspathEntriesRecorder classpathEntriesRecorder, ClasspathRecordingConfig config)
            throws IOException {
        List<String> resourcesToRecord = getResourcesToRecord(config);
        if (resourcesToRecord.isEmpty()) {
            return;
        }
        classpathEntriesRecorder.gatherAndRecord(getRecordFilePath(config).toString(),
                Phase.STATIC_INIT, resourcesToRecord);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void recordDuringRuntimeInit(ClasspathEntriesRecorder classpathEntriesRecorder, ClasspathRecordingConfig config)
            throws IOException {
        List<String> resourcesToRecord = getResourcesToRecord(config);
        if (resourcesToRecord.isEmpty()) {
            return;
        }
        classpathEntriesRecorder.gatherAndRecord(getRecordFilePath(config).toString(),
                Phase.RUNTIME_INIT, resourcesToRecord);
    }

    private static List<String> getResourcesToRecord(ClasspathRecordingConfig config) {
        return config.resources().orElse(Collections.emptyList());
    }

    /*
     * We need to record classpath entries in an external file,
     * because the application may be started multiple times with different classloaders.
     */
    public static Path getRecordFilePath(ClasspathRecordingConfig config) {
        return config.recordFile()
                .orElseThrow(() -> new IllegalStateException("Classpath entries cannot be recorded"
                        + " because application property 'quarkus.bt.classpath-recording.record-file' was not set."));
    }

}
