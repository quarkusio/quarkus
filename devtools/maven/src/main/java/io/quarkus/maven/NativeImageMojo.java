package io.quarkus.maven;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.phase.augment.AugmentTask;

/**
 * Lecacy mojo for backwards compatibility reasons. This should not be used in new projects
 *
 * This has been replaced by setting quarkus.package.types=NATIVE in the configuration.
 *
 * @deprecated
 */
@Mojo(name = "native-image", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NativeImageMojo extends AbstractMojo {

    protected static final String QUARKUS_PACKAGE_TYPES = "quarkus.package.types";
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter
    public File javaHome;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter
    private Boolean reportErrorsAtRuntime;

    @Parameter(defaultValue = "false")
    private Boolean debugSymbols;

    @Parameter(defaultValue = "${native-image.debug-build-process}")
    private Boolean debugBuildProcess;

    @Parameter(defaultValue = "true")
    private boolean publishDebugBuildProcessPort;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(defaultValue = "${native-image.new-server}")
    private Boolean cleanupServer;

    @Parameter
    private Boolean enableHttpUrlHandler;

    @Parameter
    private Boolean enableHttpsUrlHandler;

    @Parameter
    private Boolean enableAllSecurityServices;

    @Parameter
    private Boolean enableRetainedHeapReporting;

    @Parameter
    private Boolean enableIsolates;

    @Parameter
    private Boolean enableCodeSizeReporting;

    @Parameter(defaultValue = "${env.GRAALVM_HOME}")
    private String graalvmHome;

    @Parameter(defaultValue = "false")
    private Boolean enableServer;

    @Parameter(defaultValue = "false")
    private Boolean enableJni;

    @Parameter(defaultValue = "false")
    private Boolean autoServiceLoaderRegistration;

    @Parameter(defaultValue = "false")
    private Boolean dumpProxies;

    @Parameter(defaultValue = "${native-image.xmx}")
    private String nativeImageXmx;

    @Parameter(defaultValue = "${native-image.docker-build}")
    private String dockerBuild;

    @Parameter(defaultValue = "${native-image.container-runtime}")
    private String containerRuntime;

    @Parameter(defaultValue = "${native-image.container-runtime-options}")
    private String containerRuntimeOptions;

    @Parameter(defaultValue = "false")
    private Boolean enableVMInspection;

    @Parameter(defaultValue = "true")
    private Boolean fullStackTraces;

    @Parameter(defaultValue = "${native-image.disable-reports}")
    private Boolean disableReports;

    @Parameter
    private List<String> additionalBuildArgs;

    @Parameter
    private Boolean addAllCharsets;

    @Parameter
    private Boolean enableFallbackImages;

    @Parameter
    private Boolean reportExceptionStackTraces;

    /**
     * Coordinates of the Maven artifact containing the original Java application to build the native image for.
     * If not provided, the current project is assumed to be the original Java application.
     * <p>
     * The coordinates are expected to be expressed in the following format:
     * <p>
     * groupId:artifactId:classifier:type:version
     * <p>
     * With the classifier, type and version being optional.
     * <p>
     * If the type is missing, the artifact is assumed to be of type JAR.
     * <p>
     * If the version is missing, the artifact is going to be looked up among the project dependencies using the provided
     * coordinates.
     *
     * <p>
     * However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.
     *
     * <p>
     * If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type.
     */
    @Parameter(required = false, property = "quarkus.appArtifact")
    private String appArtifact;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    public NativeImageMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (project.getPackaging().equals("pom") && appArtifact == null) {
            getLog().info("Type of the artifact is POM and appArtifact parameter has not been set, skipping native-image goal");
            return;
        }

        final CuratedApplicationCreator.Builder creatorBuilder = CuratedApplicationCreator.builder();

        // The runner JAR has not been built yet, so we are going to build it
        final AppArtifact appCoords;
        AppArtifact managingProject = null;
        DefaultArtifact appMvnArtifact = null;
        if (appArtifact == null) {
            appMvnArtifact = new DefaultArtifact(project.getArtifact().getGroupId(),
                    project.getArtifact().getArtifactId(),
                    project.getArtifact().getClassifier(),
                    project.getArtifact().getArtifactHandler().getExtension(),
                    project.getArtifact().getVersion());
            appCoords = new AppArtifact(appMvnArtifact.getGroupId(), appMvnArtifact.getArtifactId(),
                    appMvnArtifact.getClassifier(), appMvnArtifact.getExtension(),
                    appMvnArtifact.getVersion());
        } else {
            final String[] coordsArr = appArtifact.split(":");
            if (coordsArr.length < 2 || coordsArr.length > 5) {
                throw new MojoExecutionException(
                        "appArtifact expression " + appArtifact
                                + " does not follow format groupId:artifactId:classifier:type:version");
            }
            final String groupId = coordsArr[0];
            final String artifactId = coordsArr[1];
            String classifier = "";
            String type = "jar";
            String version = null;
            if (coordsArr.length == 3) {
                version = coordsArr[2];
            } else if (coordsArr.length > 3) {
                classifier = coordsArr[2] == null ? "" : coordsArr[2];
                type = coordsArr[3] == null ? "jar" : coordsArr[3];
                if (coordsArr.length > 4) {
                    version = coordsArr[4];
                }
            }
            if (version == null) {
                for (Artifact dep : project.getArtifacts()) {
                    if (dep.getArtifactId().equals(artifactId)
                            && dep.getGroupId().equals(groupId)
                            && dep.getClassifier().equals(classifier)
                            && dep.getType().equals(type)) {
                        appMvnArtifact = new DefaultArtifact(dep.getGroupId(),
                                dep.getArtifactId(),
                                dep.getClassifier(),
                                dep.getArtifactHandler().getExtension(),
                                dep.getVersion());
                        break;
                    }
                }
                if (appMvnArtifact == null) {
                    throw new MojoExecutionException(
                            "Failed to locate " + appArtifact + " among the project dependencies");
                }
                appCoords = new AppArtifact(appMvnArtifact.getGroupId(), appMvnArtifact.getArtifactId(),
                        appMvnArtifact.getClassifier(), appMvnArtifact.getExtension(),
                        appMvnArtifact.getVersion());
            } else {
                appCoords = new AppArtifact(groupId, artifactId, classifier, type, version);
                appMvnArtifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
            }
            managingProject = new AppArtifact(project.getArtifact().getGroupId(),
                    project.getArtifact().getArtifactId(),
                    project.getArtifact().getClassifier(),
                    project.getArtifact().getArtifactHandler().getExtension(),
                    project.getArtifact().getVersion());
        }

        final AppModel appModel;
        final BootstrapAppModelResolver modelResolver;
        try {
            final MavenArtifactResolver mvn = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .build();
            appCoords.setPath(mvn.resolve(appMvnArtifact).getArtifact().getFile().toPath());
            modelResolver = new BootstrapAppModelResolver(mvn);
            appModel = modelResolver.resolveManagedModel(appCoords, Collections.emptyList(), managingProject);
        } catch (AppModelResolverException e) {
            throw new MojoExecutionException("Failed to resolve application model dependencies for " + appCoords, e);
        }

        final Properties buildSystemProperties = project.getProperties();
        final Properties projectProperties = new Properties();
        projectProperties.putAll(buildSystemProperties);
        projectProperties.putIfAbsent("quarkus.application.name", project.getArtifactId());
        projectProperties.putIfAbsent("quarkus.application.version", project.getVersion());

        Consumer<ConfigBuilder> config = createCustomConfig();
        String old = System.getProperty(QUARKUS_PACKAGE_TYPES);
        System.setProperty(QUARKUS_PACKAGE_TYPES, "native");

        try (CuratedApplicationCreator appCreationContext = creatorBuilder
                .setWorkDir(buildDir.toPath())
                .setModelResolver(modelResolver)
                .setBaseName(finalName)
                .setAppArtifact(appModel.getAppArtifact())
                .build()) {
            AugmentTask task = AugmentTask.builder().setConfigCustomizer(config)

                    .setAppClassesDir(new File(outputDirectory, "classes").toPath())
                    .setBuildSystemProperties(projectProperties).build();
            appCreationContext.runTask(task);
        } catch (AppCreatorException e) {
            throw new MojoExecutionException("Failed to generate a native image", e);
        } finally {
            if (old == null) {
                System.clearProperty(QUARKUS_PACKAGE_TYPES);
            } else {
                System.setProperty(QUARKUS_PACKAGE_TYPES, old);
            }
        }
    }

    private Consumer<ConfigBuilder> createCustomConfig() {
        return new Consumer<ConfigBuilder>() {
            @Override
            public void accept(ConfigBuilder configBuilder) {
                InMemoryConfigSource type = new InMemoryConfigSource(Integer.MAX_VALUE, "Native Image Type")
                        .add("quarkus.package.types", "native");
                configBuilder.withSources(type);

                InMemoryConfigSource configs = new InMemoryConfigSource(0, "Native Image Maven Settings");
                if (addAllCharsets != null) {
                    configs.add("quarkus.native.add-all-charsets", addAllCharsets.toString());
                }
                if (additionalBuildArgs != null) {
                    configs.add("quarkus.native.additional-build-args", additionalBuildArgs.toString());
                }
                if (autoServiceLoaderRegistration != null) {
                    configs.add("quarkus.native.auto-serviceloader-registration", autoServiceLoaderRegistration.toString());
                }
                if (cleanupServer != null) {
                    configs.add("quarkus.native.cleanup-server", cleanupServer.toString());
                }
                if (debugBuildProcess != null) {
                    configs.add("quarkus.native.debug-build-process", debugBuildProcess.toString());
                }
                if (debugSymbols != null) {
                    configs.add("quarkus.native.debug-symbols", debugSymbols.toString());
                }
                if (disableReports != null) {
                    configs.add("quarkus.native.disable-reports", disableReports.toString());
                }
                if (containerRuntime != null) {
                    configs.add("quarkus.native.container-runtime", containerRuntime);
                } else if (dockerBuild != null) {
                    if (!dockerBuild.isEmpty() && !dockerBuild.toLowerCase().equals("false")) {
                        if (dockerBuild.toLowerCase().equals("true")) {
                            configs.add("quarkus.native.container-runtime", "docker");
                        } else {
                            configs.add("quarkus.native.container-runtime", dockerBuild);
                        }
                    }
                }
                if (containerRuntimeOptions != null) {
                    configs.add("quarkus.native.container-runtime-options", containerRuntimeOptions);
                }
                if (dumpProxies != null) {
                    configs.add("quarkus.native.dump-proxies", dumpProxies.toString());
                }
                if (enableAllSecurityServices != null) {
                    configs.add("quarkus.native.enable-all-security-services", enableAllSecurityServices.toString());
                }
                if (enableCodeSizeReporting != null) {
                    configs.add("quarkus.native.enable-code-size-reporting", enableCodeSizeReporting.toString());
                }
                if (enableFallbackImages != null) {
                    configs.add("quarkus.native.enable-fallback-images", enableFallbackImages.toString());
                }
                if (enableHttpsUrlHandler != null) {
                    configs.add("quarkus.native.enable-https-url-handler", enableHttpsUrlHandler.toString());
                }
                if (enableHttpUrlHandler != null) {
                    configs.add("quarkus.native.enable-http-url-handler", enableHttpUrlHandler.toString());
                }
                if (enableIsolates != null) {
                    configs.add("quarkus.native.enable-isolates", enableIsolates.toString());
                }
                if (enableJni != null) {
                    configs.add("quarkus.native.enable-jni", enableJni.toString());
                }
                if (enableRetainedHeapReporting != null) {
                    configs.add("quarkus.native.enable-retained-heap-reporting", enableRetainedHeapReporting.toString());
                }
                if (enableServer != null) {
                    configs.add("quarkus.native.enable-server", enableServer.toString());
                }
                if (enableVMInspection != null) {
                    configs.add("quarkus.native.enable-vm-inspection", enableVMInspection.toString());
                }
                if (fullStackTraces != null) {
                    configs.add("quarkus.native.full-stack-traces", fullStackTraces.toString());
                }
                if (graalvmHome != null) {
                    configs.add("quarkus.native.graalvm-home", graalvmHome.toString());
                }
                if (javaHome != null) {
                    configs.add("quarkus.native.java-home", javaHome.toString());
                }
                if (nativeImageXmx != null) {
                    configs.add("quarkus.native.native-image-xmx", nativeImageXmx.toString());
                }
                if (reportErrorsAtRuntime != null) {
                    configs.add("quarkus.native.report-errors-at-runtime", reportErrorsAtRuntime.toString());
                }
                if (reportExceptionStackTraces != null) {
                    configs.add("quarkus.native.report-exception-stack-traces", reportExceptionStackTraces.toString());
                }
                if (publishDebugBuildProcessPort) {
                    configs.add("quarkus.native-image.publish-debug-build-process-port",
                            Boolean.toString(publishDebugBuildProcessPort));
                }
                configBuilder.withSources(configs);

            }
        };

    }

    private static final class InMemoryConfigSource implements ConfigSource {

        private final Map<String, String> values = new HashMap<>();
        private final int ordinal;
        private final String name;

        private InMemoryConfigSource(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }

        public InMemoryConfigSource add(String key, String value) {
            values.put(key, value);
            return this;
        }

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
