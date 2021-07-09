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
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.extest.runtime.classpath.ClasspathEntriesRecorder;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries;
import io.quarkus.extest.runtime.classpath.RecordedClasspathEntries.Phase;
import io.quarkus.extest.runtime.config.TestBuildTimeConfig;

public class ClasspathEntryRecordingBuildStep {
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerRecordedClasspathEntries(ClasspathEntriesRecorder classpathEntriesRecorder,
            TestBuildTimeConfig config, BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        Optional<Path> recordFilePath = config.classpathRecording.recordFile;
        if (!recordFilePath.isPresent()) {
            return;
        }
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(RecordedClasspathEntries.class)
                .runtimeValue(classpathEntriesRecorder.recordedClasspathEntries(recordFilePath.get().toString().toString()))
                .done());
    }

    @BuildStep
    // This makes sure we execute this step even though it doesn't produce anything useful for the build
    // (just side-effects).
    @Produce(FeatureBuildItem.class)
    // This makes sure we execute this in io.quarkus.test.junit.IntegrationTestUtil.handleDevDb,
    // so that we can reproduce a problem that happens in the Hibernate ORM extension.
    @Produce(DevServicesNativeConfigResultBuildItem.class)
    void recordDuringAugmentation(TestBuildTimeConfig config)
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
    // This makes sure we execute this in io.quarkus.test.junit.IntegrationTestUtil.handleDevDb,
    // so that we can reproduce a problem that happens in the Hibernate ORM extension.
    @Produce(DevServicesNativeConfigResultBuildItem.class)
    void recordDuringStaticInit(ClasspathEntriesRecorder classpathEntriesRecorder, TestBuildTimeConfig config)
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
    // This makes sure we execute this in io.quarkus.test.junit.IntegrationTestUtil.handleDevDb,
    // so that we can reproduce a problem that happens in the Hibernate ORM extension.
    @Produce(DevServicesNativeConfigResultBuildItem.class)
    void recordDuringRuntimeInit(ClasspathEntriesRecorder classpathEntriesRecorder, TestBuildTimeConfig config)
            throws IOException {
        List<String> resourcesToRecord = getResourcesToRecord(config);
        if (resourcesToRecord.isEmpty()) {
            return;
        }
        classpathEntriesRecorder.gatherAndRecord(getRecordFilePath(config).toString(),
                Phase.RUNTIME_INIT, resourcesToRecord);
    }

    private static List<String> getResourcesToRecord(TestBuildTimeConfig config) {
        return config.classpathRecording.resources.orElse(Collections.emptyList());
    }

    /*
     * We need to record classpath entries in an external file,
     * because the application may be started multiple times with different classloaders.
     */
    public static Path getRecordFilePath(TestBuildTimeConfig config) {
        return config.classpathRecording.recordFile
                .orElseThrow(() -> new IllegalStateException("Classpath entries cannot be recorded"
                        + " because application property 'quarkus.bt.classpath-recording.record-file' was not set."));
    }

}
