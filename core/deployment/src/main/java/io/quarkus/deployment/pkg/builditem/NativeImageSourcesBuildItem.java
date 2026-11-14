package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Describes the native image sources produced when {@code quarkus.native.sources-only=true}.
 * <p>
 * In that mode Quarkus does not invoke {@code native-image} itself. Instead, it writes everything needed to do so later
 * into the {@code native-sources} directory: the runner jar and its libraries, the {@code native-image} arguments
 * ({@code native-image.args}), the GraalVM version the arguments were generated for ({@code graalvm.version}) and,
 * for container builds, the builder image ({@code native-builder.image}).
 * <p>
 * This build item exposes the same information so that extensions can run follow-up work on the native sources
 * (for example, building them on other machines or for other platforms) without having to depend on, or re-parse,
 * the layout and format of the files in that directory.
 * <p>
 * It is only produced for native sources builds, i.e. it is not available for regular native or JVM builds.
 */
public final class NativeImageSourcesBuildItem extends SimpleBuildItem {

    private final Path directory;
    private final String runnerJarName;
    private final String nativeImageName;
    private final List<String> nativeImageArgs;
    private final String graalVMVersion;
    private final Optional<String> builderImage;

    public NativeImageSourcesBuildItem(Path directory, String runnerJarName, String nativeImageName,
            List<String> nativeImageArgs, String graalVMVersion, Optional<String> builderImage) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.runnerJarName = Objects.requireNonNull(runnerJarName, "runnerJarName");
        this.nativeImageName = Objects.requireNonNull(nativeImageName, "nativeImageName");
        this.nativeImageArgs = List.copyOf(nativeImageArgs);
        this.graalVMVersion = Objects.requireNonNull(graalVMVersion, "graalVMVersion");
        this.builderImage = Objects.requireNonNull(builderImage, "builderImage");
    }

    /**
     * @return the {@code native-sources} directory, which is also the working directory {@code native-image} is expected
     *         to be invoked from with {@link #getNativeImageArgs()}
     */
    public Path getDirectory() {
        return directory;
    }

    /**
     * @return the file name of the runner jar, located in {@link #getDirectory()}
     */
    public String getRunnerJarName() {
        return runnerJarName;
    }

    /**
     * @return the name of the native executable that {@code native-image} will produce (without any platform specific
     *         extension such as {@code .exe})
     */
    public String getNativeImageName() {
        return nativeImageName;
    }

    /**
     * @return the arguments to pass to {@code native-image}, in order; these are the same tokens that are written,
     *         space-separated, to {@code native-image.args}. The list is unmodifiable.
     */
    public List<String> getNativeImageArgs() {
        return nativeImageArgs;
    }

    /**
     * @return the GraalVM / Mandrel version the arguments were generated for, as written to {@code graalvm.version}
     */
    public String getGraalVMVersion() {
        return graalVMVersion;
    }

    /**
     * @return the builder image to use when the native sources are meant to be built in a container, as written to
     *         {@code native-builder.image}; empty when the build is not a container build
     */
    public Optional<String> getBuilderImage() {
        return builderImage;
    }
}
