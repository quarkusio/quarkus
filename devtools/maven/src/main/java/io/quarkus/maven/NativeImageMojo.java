package io.quarkus.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.phase.augment.AugmentOutcome;
import io.quarkus.creator.phase.augment.AugmentPhase;
import io.quarkus.creator.phase.curate.CurateOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImageOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImagePhase;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;
import io.quarkus.creator.phase.runnerjar.RunnerJarPhase;

/**
 * Build a native executable of your application.
 */
@Mojo(name = "native-image", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NativeImageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/wiring-classes")
    private File wiringClassesDirectory;

    @Parameter(defaultValue = "false")
    private boolean reportErrorsAtRuntime;

    @Parameter(defaultValue = "false")
    private boolean debugSymbols;

    @Parameter(defaultValue = "${native-image.debug-build-process}")
    private boolean debugBuildProcess;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(defaultValue = "${native-image.new-server}")
    private boolean cleanupServer;

    @Parameter
    private boolean enableHttpUrlHandler;

    @Parameter
    private boolean enableHttpsUrlHandler;

    @Parameter
    private boolean enableAllSecurityServices;

    @Parameter
    private boolean enableRetainedHeapReporting;

    @Parameter
    private boolean enableIsolates;

    @Parameter
    private boolean enableCodeSizeReporting;

    @Parameter(defaultValue = "${env.GRAALVM_HOME}")
    private String graalvmHome;

    @Parameter(defaultValue = "false")
    private boolean enableServer;

    @Parameter(defaultValue = "false")
    private boolean enableJni;

    @Parameter(defaultValue = "false")
    private boolean autoServiceLoaderRegistration;

    @Parameter(defaultValue = "false")
    private boolean dumpProxies;

    @Parameter(defaultValue = "${native-image.xmx}")
    private String nativeImageXmx;

    @Parameter(defaultValue = "${native-image.docker-build}")
    private String dockerBuild;

    @Parameter(defaultValue = "${native-image.container-runtime}")
    private String containerRuntime;

    @Parameter(defaultValue = "${native-image.container-runtime-options}")
    private String containerRuntimeOptions;

    @Parameter(defaultValue = "false")
    private boolean enableVMInspection;

    @Parameter(defaultValue = "true")
    private boolean fullStackTraces;

    @Parameter(defaultValue = "${native-image.disable-reports}")
    private boolean disableReports;

    @Parameter
    private List<String> additionalBuildArgs;

    @Parameter(defaultValue = "false")
    private boolean addAllCharsets;

    @Parameter(defaultValue = "false")
    private boolean enableFallbackImages;

    @Parameter(defaultValue = "true")
    private boolean reportExceptionStackTraces;

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

        final AppCreator.Builder creatorBuilder = AppCreator.builder();
        CurateOutcome curateOutcome = null;
        AugmentOutcome augmentOutcome = null;
        RunnerJarOutcome runnerJarOutcome = null;

        if (!buildDir.isDirectory() || !new File(buildDir, "lib").isDirectory()) {
            // The runner JAR has not been built yet, so we are going to build it
            final AppArtifact appCoords;
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
                appModel = modelResolver.resolveModel(appCoords);
            } catch (AppModelResolverException e) {
                throw new MojoExecutionException("Failed to resolve application model dependencies for " + appCoords, e);
            }

            creatorBuilder.addPhase(new AugmentPhase()
                    .setAppClassesDir(new File(outputDirectory, "classes").toPath())
                    .setWiringClassesDir(wiringClassesDirectory.toPath()))
                    .addPhase(new RunnerJarPhase()
                            .setFinalName(finalName))
                    .setWorkDir(buildDir.toPath());

            curateOutcome = CurateOutcome.builder()
                    .setAppModelResolver(modelResolver)
                    .setAppModel(appModel)
                    .build();
        } else {
            // the runner JAR is already available, so we are going to re-use it
            augmentOutcome = new AugmentOutcome() {
                final Path classesDir = new File(outputDirectory, "classes").toPath();

                @Override
                public Path getAppClassesDir() {
                    return classesDir;
                }

                @Override
                public Path getTransformedClassesDir() {
                    // not relevant in this context
                    throw new UnsupportedOperationException();
                }

                @Override
                public Path getWiringClassesDir() {
                    // not relevant in this context
                    throw new UnsupportedOperationException();
                }

                @Override
                public Path getConfigDir() {
                    return classesDir;
                }

                @Override
                public Map<Path, Set<String>> getTransformedClassesByJar() {
                    return Collections.emptyMap();
                }
            };

            runnerJarOutcome = new RunnerJarOutcome() {
                final Path runnerJar = buildDir.toPath().resolve(finalName + "-runner.jar");
                final Path originalJar = buildDir.toPath().resolve(finalName + ".jar");

                @Override
                public Path getRunnerJar() {
                    return runnerJar;
                }

                @Override
                public Path getLibDir() {
                    return runnerJar.getParent().resolve("lib");
                }

                @Override
                public Path getOriginalJar() {
                    return originalJar;
                }
            };
        }

        creatorBuilder.addPhase(new NativeImagePhase()
                .setAddAllCharsets(addAllCharsets)
                .setAdditionalBuildArgs(additionalBuildArgs)
                .setAutoServiceLoaderRegistration(autoServiceLoaderRegistration)
                .setOutputDir(buildDir.toPath())
                .setCleanupServer(cleanupServer)
                .setDebugBuildProcess(debugBuildProcess)
                .setDebugSymbols(debugSymbols)
                .setDisableReports(disableReports)
                .setDockerBuild(dockerBuild)
                .setContainerRuntime(containerRuntime)
                .setContainerRuntimeOptions(containerRuntimeOptions)
                .setDumpProxies(dumpProxies)
                .setEnableAllSecurityServices(enableAllSecurityServices)
                .setEnableCodeSizeReporting(enableCodeSizeReporting)
                .setEnableFallbackImages(enableFallbackImages)
                .setEnableHttpsUrlHandler(enableHttpsUrlHandler)
                .setEnableHttpUrlHandler(enableHttpUrlHandler)
                .setEnableIsolates(enableIsolates)
                .setEnableJni(enableJni)
                .setEnableRetainedHeapReporting(enableRetainedHeapReporting)
                .setEnableServer(enableServer)
                .setEnableVMInspection(enableVMInspection)
                .setFullStackTraces(fullStackTraces)
                .setGraalvmHome(graalvmHome)
                .setNativeImageXmx(nativeImageXmx)
                .setReportErrorsAtRuntime(reportErrorsAtRuntime)
                .setReportExceptionStackTraces(reportExceptionStackTraces));

        try (AppCreator appCreator = creatorBuilder.build()) {
            if (curateOutcome != null) {
                appCreator.pushOutcome(CurateOutcome.class, curateOutcome);
            }
            if (augmentOutcome != null) {
                appCreator.pushOutcome(AugmentOutcome.class, augmentOutcome);
            }
            if (runnerJarOutcome != null) {
                appCreator.pushOutcome(RunnerJarOutcome.class, runnerJarOutcome);
            }
            appCreator.resolveOutcome(NativeImageOutcome.class);
        } catch (AppCreatorException e) {
            throw new MojoExecutionException("Failed to generate a native image", e);
        }
    }
}
