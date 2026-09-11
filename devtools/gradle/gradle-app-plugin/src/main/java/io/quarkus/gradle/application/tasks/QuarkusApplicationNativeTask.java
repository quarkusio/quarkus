package io.quarkus.gradle.application.tasks;

import java.nio.file.Path;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;

/**
 * Produces a native executable or native sources for a named application build.
 * <p>
 * This task is the native-build implementation behind the plugin-generated named-build task. Native arguments are
 * derived from the named-build DSL and passed as operation-forced Quarkus properties; the task property is wiring, not
 * a separate user configuration surface. The task writes a result descriptor but is not build-cacheable yet.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Native image builds are not build-cacheable yet")
public abstract class QuarkusApplicationNativeTask extends QuarkusApplicationBuildTask {

    /**
     * Returns the operation-forced Quarkus properties derived from native-build configuration.
     *
     * @return the native operation properties
     */
    @Input
    public abstract MapProperty<String, String> getNativeArguments();

    /**
     * Returns the native-build result descriptor written by the task.
     *
     * @return the native result file
     */
    @OutputFile
    public abstract RegularFileProperty getNativeResultFile();

    /**
     * Runs native augmentation and records the resulting executable or source artifact.
     */
    @TaskAction
    public void buildNativeImage() {
        Path nativeResultFile = getNativeResultFile().get().getAsFile().toPath();
        Path augmentResultFile = nativeResultFile.resolveSibling("native-augmentation-result.properties");
        var result = buildOperations().buildNative(buildRequest(getNativeArguments().get()), augmentResultFile);
        new NativeResultCodec().write(nativeResultFile, result);
    }
}
