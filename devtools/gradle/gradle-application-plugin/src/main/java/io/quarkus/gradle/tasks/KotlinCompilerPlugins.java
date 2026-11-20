package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.verification.DependencyVerificationMode;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.logging.Logger;
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig;
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin;
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption;
import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile;

import io.quarkus.deployment.dev.DevModeCommandLineBuilder;

/**
 * Collects the Kotlin compiler plugins configured through the Kotlin Gradle plugin of a Kotlin JVM project in the
 * current build.
 * <p>
 * This class references the Kotlin Gradle plugin API, so it must only be loaded when the Kotlin Gradle plugin is applied.
 */
final class KotlinCompilerPlugins {

    private static final String COMPILE_KOTLIN_TASK_NAME = "compileKotlin";
    /**
     * The configuration the Kotlin Gradle plugin builds the plugin classpath of {@code compileKotlin} from.
     */
    private static final String PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspathMain";
    private static final String KOTLIN_GROUP_ID = "org.jetbrains.kotlin";
    private static final String EMBEDDABLE_SUFFIX = "-embeddable";
    /**
     * The scripting plugin, which the Kotlin Gradle plugin always adds. Dev mode does not compile scripts.
     */
    private static final String KOTLIN_SCRIPTING_ARTIFACT_PREFIX = "kotlin-scripting-";
    private static final String KOTLIN_SCRIPTING_PLUGIN_ID = "kotlin.scripting";
    /**
     * kapt and KSP add their plugins to the compiler plugin classpath, but run in their own tasks.
     */
    private static final String KAPT_ARTIFACT_ID = "kotlin-annotation-processing-gradle";
    private static final String KSP_GROUP_ID = "com.google.devtools.ksp";
    private static final String VERIFICATION_METADATA_PATH = "gradle/verification-metadata.xml";
    private static final String SERVICES = "META-INF/services/";

    private KotlinCompilerPlugins() {
    }

    /**
     * Passes the compiler plugins and the compiler plugin options of the {@code compileKotlin} task of the project to
     * the dev mode compiler.
     * <p>
     * Dev mode compiles with {@code kotlin-compiler}, while the Kotlin Gradle plugin uses the {@code -embeddable}
     * variants of the compiler plugins, which are built for {@code kotlin-compiler-embeddable}. The matching
     * non-embeddable compiler plugins are therefore resolved and passed instead. Nothing is passed if the Kotlin Gradle
     * plugin version differs from the version of the dev mode compiler, or if a compiler plugin cannot be used.
     *
     * @param project the project with the Kotlin JVM plugin applied
     * @param compilerVersion the version of the Kotlin compiler used by dev mode
     * @param builder the dev mode command line builder
     * @param logger the logger of the task
     */
    static void configure(Project project, String compilerVersion, DevModeCommandLineBuilder builder, Logger logger) {
        final Task task = project.getTasks().findByName(COMPILE_KOTLIN_TASK_NAME);
        if (!(task instanceof BaseKotlinCompile compileKotlin)) {
            logger.debug("No Kotlin {} task, Kotlin compiler plugins are not passed to dev mode", COMPILE_KOTLIN_TASK_NAME);
            return;
        }
        final String pluginVersion = getKotlinPluginVersion(project);
        if (!compilerVersion.equals(pluginVersion)) {
            warnNotApplied(logger, "the Kotlin Gradle plugin version " + pluginVersion
                    + " differs from the Kotlin version " + compilerVersion + " of the Quarkus platform (use the Kotlin Gradle"
                    + " plugin " + compilerVersion + " to apply them)");
            return;
        }
        final List<String> pluginArtifacts = getNonEmbeddablePluginArtifacts(project, compileKotlin, compilerVersion,
                logger);
        if (pluginArtifacts == null) {
            return;
        }

        final List<String> pluginOptions = new ArrayList<>();
        for (CompilerPluginConfig config : compileKotlin.getPluginOptions().get()) {
            for (Map.Entry<String, List<SubpluginOption>> e : config.allOptions().entrySet()) {
                if (KOTLIN_SCRIPTING_PLUGIN_ID.equals(e.getKey())) {
                    continue;
                }
                for (SubpluginOption option : e.getValue()) {
                    // same format as the kotlin-maven-plugin pluginOptions, see KotlinCompilationProvider
                    pluginOptions.add(e.getKey() + ":" + option.getKey() + "=" + option.getValue());
                }
            }
        }
        if (!pluginArtifacts.isEmpty()) {
            builder.compilerPluginArtifacts(pluginArtifacts);
        }
        if (!pluginOptions.isEmpty()) {
            builder.compilerPluginOptions(pluginOptions);
        }
    }

    /**
     * Resolves the non-embeddable variants of the compiler plugins on the plugin classpath of the compile task.
     *
     * @param project the project with the Kotlin JVM plugin applied
     * @param compileKotlin the Kotlin compile task
     * @param compilerVersion the version of the Kotlin compiler used by dev mode
     * @param logger the logger of the task
     * @return the paths of the compiler plugin artifacts, or {@code null} if not all of them can be used in dev mode
     */
    private static List<String> getNonEmbeddablePluginArtifacts(Project project, BaseKotlinCompile compileKotlin,
            String compilerVersion, Logger logger) {
        final Configuration pluginConfiguration = project.getConfigurations()
                .findByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME);
        if (pluginConfiguration == null) {
            warnNotApplied(logger, "the layout of the Kotlin Gradle plugin is not recognised");
            return null;
        }
        final Map<File, ModuleComponentIdentifier> moduleIds = new HashMap<>();
        for (ResolvedArtifactResult artifact : pluginConfiguration.getIncoming().getArtifacts()) {
            if (artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier id) {
                moduleIds.put(artifact.getFile(), id);
            }
        }

        final List<Dependency> nonEmbeddable = new ArrayList<>();
        final List<String> unsupported = new ArrayList<>();
        for (File file : compileKotlin.getPluginClasspath().getFiles()) {
            final ModuleComponentIdentifier id = moduleIds.get(file);
            if (id != null && isNotNeeded(id)) {
                continue;
            } else if (id != null && KOTLIN_GROUP_ID.equals(id.getGroup()) && id.getModule().endsWith(EMBEDDABLE_SUFFIX)) {
                final String module = id.getModule().substring(0, id.getModule().length() - EMBEDDABLE_SUFFIX.length());
                nonEmbeddable.add(project.getDependencies().create(KOTLIN_GROUP_ID + ":" + module + ":" + compilerVersion));
            } else if (isCompilerPlugin(file)) {
                unsupported.add(id == null ? file.getName() : id.getDisplayName());
            }
            // other jars are libraries of compiler plugins, the non-embeddable JetBrains plugins do not need them
        }
        if (!unsupported.isEmpty()) {
            warnNotApplied(logger, unsupported + " cannot be used by the dev mode compiler");
            return null;
        }
        if (nonEmbeddable.isEmpty()) {
            return List.of();
        }
        if (isStrictDependencyVerification(project)) {
            // a verification failure would fail the whole build, not only this resolution
            warnNotApplied(logger, "strict dependency verification is enabled");
            return null;
        }

        final Configuration configuration = project.getConfigurations()
                .detachedConfiguration(nonEmbeddable.toArray(new Dependency[0]));
        configuration.setTransitive(false);
        configuration.attributes(attributes -> {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
        });
        final ArtifactCollection artifacts = configuration.getIncoming().artifactView(view -> view.setLenient(true))
                .getArtifacts();
        final List<String> paths = new ArrayList<>(nonEmbeddable.size());
        for (ResolvedArtifactResult artifact : artifacts.getArtifacts()) {
            paths.add(artifact.getFile().getAbsolutePath());
        }
        if (!artifacts.getFailures().isEmpty() || paths.size() != nonEmbeddable.size()) {
            final List<String> requested = new ArrayList<>(nonEmbeddable.size());
            for (Dependency dependency : nonEmbeddable) {
                requested.add(dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion());
            }
            warnNotApplied(logger, requested + " could not be resolved");
            for (Throwable failure : artifacts.getFailures()) {
                logger.debug("Failed to resolve a Kotlin compiler plugin", failure);
            }
            return null;
        }
        return paths;
    }

    /**
     * Whether a compiler plugin is not needed by the dev mode compiler: the scripting plugin and the kapt and KSP plugins.
     * They register compiler plugins, so they have to be left out explicitly.
     */
    private static boolean isNotNeeded(ModuleComponentIdentifier id) {
        return KSP_GROUP_ID.equals(id.getGroup()) || KOTLIN_GROUP_ID.equals(id.getGroup())
                && (id.getModule().startsWith(KOTLIN_SCRIPTING_ARTIFACT_PREFIX) || KAPT_ARTIFACT_ID.equals(id.getModule()));
    }

    /**
     * Whether the jar registers a Kotlin compiler plugin. Anything that cannot be read is assumed to be one.
     */
    private static boolean isCompilerPlugin(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            return zip.getEntry(SERVICES + "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar") != null
                    || zip.getEntry(SERVICES + "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar") != null;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Whether strict dependency verification is enabled and not in the process of writing the verification metadata.
     */
    private static boolean isStrictDependencyVerification(Project project) {
        final StartParameter startParameter = project.getGradle().getStartParameter();
        return startParameter.getDependencyVerificationMode() == DependencyVerificationMode.STRICT
                && startParameter.getWriteDependencyVerifications().isEmpty()
                && new File(project.getRootDir(), VERIFICATION_METADATA_PATH).isFile();
    }

    private static void warnNotApplied(Logger logger, String reason) {
        logger.warn("Kotlin compiler plugins (e.g. all-open) are not applied when recompiling in dev mode, because {}."
                + " Disable this with quarkusDev { shouldPropagateKotlinCompilerPlugins(false) }", reason);
    }

    /**
     * Returns the version of the Kotlin Gradle plugin applied to the project.
     *
     * @param project the project with the Kotlin JVM plugin applied
     * @return the Kotlin Gradle plugin version, or {@code null} if it cannot be determined
     */
    private static String getKotlinPluginVersion(Project project) {
        for (KotlinBasePlugin plugin : project.getPlugins().withType(KotlinBasePlugin.class)) {
            return plugin.getPluginVersion();
        }
        return null;
    }
}
