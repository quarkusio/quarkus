package io.quarkus.gradle.application.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import io.quarkus.gradle.application.internal.planning.PackageOutputName;
import io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

/**
 * Registers the named application outputs in the {@code quarkusApplication.builds} block.
 * <p>
 * Registration is lazy and each name must map to a unique generated Gradle task-name segment. A name must start with a
 * letter, contain only letters, digits, hyphens, or underscores, and must not contain empty segments. Each convenience
 * method fixes the output type and returns a {@link NamedDomainObjectProvider}; declaring an output registers its task
 * family but does not select any task for execution.
 */
public class QuarkusApplicationBuilds {

    private final ExtensiblePolymorphicDomainObjectContainer<QuarkusApplicationBuild> container;
    private final ProviderFactory providers;
    private final String projectName;
    private final Provider<String> projectVersion;
    private final DslLifecycleCoordinator lifecycle;
    private final Map<String, QuarkusApplicationJvmStartupArchiveType> initialAotJarTypes = new HashMap<>();

    /**
     * Creates the Gradle-managed polymorphic build container and its output conventions.
     *
     * @param objects Gradle's object factory
     * @param providers Gradle's provider factory
     * @param layout the project layout
     * @param projectName the project name used as the archive-base-name convention
     * @param projectVersion the lazy project version used as the archive-version convention
     * @param lifecycle the plugin's internal DSL lifecycle coordinator
     */
    @Inject
    public QuarkusApplicationBuilds(ObjectFactory objects, ProviderFactory providers, ProjectLayout layout,
            String projectName, Provider<String> projectVersion, Object lifecycle) {
        this.providers = providers;
        this.projectName = projectName;
        this.projectVersion = projectVersion;
        if (!(lifecycle instanceof DslLifecycleCoordinator coordinator)) {
            throw new IllegalArgumentException("Quarkus application builds require their internal lifecycle coordinator");
        }
        this.lifecycle = coordinator;
        this.container = objects.polymorphicDomainObjectContainer(QuarkusApplicationBuild.class);
        registerFactories(objects, layout);
    }

    private void registerFactories(ObjectFactory objects, ProjectLayout layout) {
        container.registerFactory(QuarkusFastJarOutput.class,
                name -> newBuild(objects, layout, QuarkusFastJarOutput.class, name));
        container.registerFactory(QuarkusAotJarOutput.class, name -> {
            QuarkusAotJarOutput build = objects.newInstance(
                    QuarkusAotJarOutput.class, name, layout, objects, lifecycle);
            configureBuildConventions(build);
            QuarkusApplicationJvmStartupArchiveType initialType = initialAotJarTypes.remove(name);
            if (initialType != null) {
                build.getStartupArchive().getType().set(initialType);
                build.getStartupArchive().getType().disallowChanges();
            }
            return build;
        });
        container.registerFactory(QuarkusLegacyJarOutput.class,
                name -> newBuild(objects, layout, QuarkusLegacyJarOutput.class, name));
        container.registerFactory(QuarkusMutableJarOutput.class,
                name -> newBuild(objects, layout, QuarkusMutableJarOutput.class, name));
        container.registerFactory(QuarkusUberJarOutput.class,
                name -> newBuild(objects, layout, QuarkusUberJarOutput.class, name));
        container.registerFactory(QuarkusNativeOutput.class,
                name -> newBuild(objects, layout, QuarkusNativeOutput.class, name));
        container.registerFactory(QuarkusNativeSourcesOutput.class,
                name -> newBuild(objects, layout, QuarkusNativeSourcesOutput.class, name));
    }

    private <T extends QuarkusApplicationBuild> T newBuild(ObjectFactory objects, ProjectLayout layout, Class<T> type,
            String name) {
        T build = objects.newInstance(type, name, layout, objects);
        configureBuildConventions(build);
        return build;
    }

    private void configureBuildConventions(QuarkusApplicationBuild build) {
        build.getArchiveBaseName().convention(projectName);
        build.getArchiveBaseNameSuffix().convention("");
        build.getArchiveVersion().convention(projectVersion);
        build.getOutputName().convention(providers.provider(() -> PackageOutputName.assemble(
                build.getArchiveBaseName().get(),
                build.getArchiveBaseNameSuffix().get(),
                build.getArchiveVersion().get())));
        if (build instanceof QuarkusApplicationRunnerOutput runnerOutput) {
            runnerOutput.getArchiveRunnerSuffix().convention("-runner");
            runnerOutput.getArchiveAddRunnerSuffix().convention(true);
        }
    }

    /**
     * Lazily registers a fast-JAR output.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusFastJarOutput> fastJar(String name) {
        return register(name, QuarkusFastJarOutput.class);
    }

    /**
     * Lazily registers and configures a fast-JAR output.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusFastJarOutput> fastJar(String name,
            Action<? super QuarkusFastJarOutput> action) {
        return register(name, QuarkusFastJarOutput.class, action);
    }

    /**
     * Registers the Quarkus AOT-JAR package layout without selecting or
     * producing a concrete JVM startup archive.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusAotJarOutput> aotJar(String name) {
        return register(name, QuarkusAotJarOutput.class);
    }

    /**
     * Registers the Quarkus AOT-JAR package layout without implicitly
     * selecting OpenJDK AOT, SCC, or AppCDS.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusAotJarOutput> aotJar(String name,
            Action<? super QuarkusAotJarOutput> action) {
        return register(name, QuarkusAotJarOutput.class, action);
    }

    /**
     * Registers the historically named Quarkus AOT-JAR layout and fixes the
     * concrete JVM startup archive type. The layout supports AOT, SCC, and
     * AppCDS archives.
     *
     * @param name the build name
     * @param type the required startup archive type
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusAotJarOutput> aotJar(String name,
            QuarkusApplicationJvmStartupArchiveType type) {
        return aotJar(name, type, ignored -> {
        });
    }

    /**
     * Registers the historically named Quarkus AOT-JAR layout and fixes the
     * concrete JVM startup archive type before applying the action.
     *
     * @param name the build name
     * @param type the required startup archive type
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusAotJarOutput> aotJar(String name,
            QuarkusApplicationJvmStartupArchiveType type,
            Action<? super QuarkusAotJarOutput> action) {
        Objects.requireNonNull(type, "type");
        if (!container.getNames().contains(name)) {
            initialAotJarTypes.put(name, type);
        }
        return register(name, QuarkusAotJarOutput.class, action);
    }

    /**
     * Lazily registers a legacy-JAR output.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusLegacyJarOutput> legacyJar(String name) {
        return register(name, QuarkusLegacyJarOutput.class);
    }

    /**
     * Lazily registers and configures a legacy-JAR output.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusLegacyJarOutput> legacyJar(String name,
            Action<? super QuarkusLegacyJarOutput> action) {
        return register(name, QuarkusLegacyJarOutput.class, action);
    }

    /**
     * Lazily registers a mutable-JAR output.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusMutableJarOutput> mutableJar(String name) {
        return register(name, QuarkusMutableJarOutput.class);
    }

    /**
     * Lazily registers and configures a mutable-JAR output.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusMutableJarOutput> mutableJar(String name,
            Action<? super QuarkusMutableJarOutput> action) {
        return register(name, QuarkusMutableJarOutput.class, action);
    }

    /**
     * Lazily registers an uber-JAR output.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusUberJarOutput> uberJar(String name) {
        return register(name, QuarkusUberJarOutput.class);
    }

    /**
     * Lazily registers and configures an uber-JAR output.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusUberJarOutput> uberJar(String name,
            Action<? super QuarkusUberJarOutput> action) {
        return register(name, QuarkusUberJarOutput.class, action);
    }

    /**
     * Lazily registers a native-executable output.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusNativeOutput> nativeExecutable(String name) {
        return register(name, QuarkusNativeOutput.class);
    }

    /**
     * Lazily registers and configures a native-executable output.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusNativeOutput> nativeExecutable(String name,
            Action<? super QuarkusNativeOutput> action) {
        return register(name, QuarkusNativeOutput.class, action);
    }

    /**
     * Lazily registers a native-sources output.
     *
     * @param name the build name
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusNativeSourcesOutput> nativeSources(String name) {
        return register(name, QuarkusNativeSourcesOutput.class);
    }

    /**
     * Lazily registers and configures a native-sources output.
     *
     * @param name the build name
     * @param action the output configuration action
     * @return a provider for the named output
     */
    public NamedDomainObjectProvider<QuarkusNativeSourcesOutput> nativeSources(String name,
            Action<? super QuarkusNativeSourcesOutput> action) {
        return register(name, QuarkusNativeSourcesOutput.class, action);
    }

    /**
     * Lazily registers one of the plugin's supported concrete named-build types.
     *
     * @param name the build name
     * @param type the concrete build DSL type
     * @param <T> the build DSL type
     * @return a provider for the named output
     */
    public <T extends QuarkusApplicationBuild> NamedDomainObjectProvider<T> register(String name, Class<T> type) {
        return container.register(name, type);
    }

    /**
     * Lazily registers and configures one of the plugin's supported concrete named-build types.
     *
     * @param name the build name
     * @param type the concrete build DSL type
     * @param action the output configuration action
     * @param <T> the build DSL type
     * @return a provider for the named output
     */
    public <T extends QuarkusApplicationBuild> NamedDomainObjectProvider<T> register(String name, Class<T> type,
            Action<? super T> action) {
        return container.register(name, type, action);
    }

    /**
     * Configures every present and future named build.
     *
     * @param action the build configuration action
     */
    public void all(Action<? super QuarkusApplicationBuild> action) {
        container.all(action);
    }

    /**
     * Configures this build container.
     * <p>
     * This callable shape supports nested Groovy and Kotlin action syntax without exposing the underlying container.
     *
     * @param action the container configuration action
     */
    public void configure(Action<? super QuarkusApplicationBuilds> action) {
        action.execute(this);
    }
}
