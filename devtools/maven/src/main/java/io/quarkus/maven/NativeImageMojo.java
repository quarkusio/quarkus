package io.quarkus.maven;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

/**
 * Legacy mojo for backwards compatibility reasons. This should not be used in new projects
 *
 * This has been replaced by setting quarkus.package.type=native in the configuration.
 *
 * @deprecated
 */
@Mojo(name = "native-image", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Deprecated
public class NativeImageMojo extends AbstractMojo {

    protected static final String QUARKUS_PACKAGE_TYPE = "quarkus.package.type";
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter
    public File javaHome;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
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
    private Boolean enableIsolates;

    @Parameter(defaultValue = "${env.GRAALVM_HOME}")
    private String graalvmHome;

    @Parameter(defaultValue = "false")
    private Boolean enableServer;

    /**
     * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Deprecated
    @Parameter(defaultValue = "true")
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

    @Deprecated
    @Parameter(defaultValue = "${native-image.disable-reports}")
    private Boolean disableReports;

    @Parameter(defaultValue = "${native-image.enable-reports}")
    private Boolean enableReports;

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
        try {

            final Properties projectProperties = project.getProperties();
            final Properties realProperties = new Properties();
            for (String name : projectProperties.stringPropertyNames()) {
                if (name.startsWith("quarkus.")) {
                    realProperties.setProperty(name, projectProperties.getProperty(name));
                }
            }
            realProperties.putIfAbsent("quarkus.application.name", project.getArtifactId());
            realProperties.putIfAbsent("quarkus.application.version", project.getVersion());

            Map<String, String> config = createCustomConfig();
            Map<String, String> old = new HashMap<>();
            for (Map.Entry<String, String> e : config.entrySet()) {
                old.put(e.getKey(), System.getProperty(e.getKey()));
                System.setProperty(e.getKey(), e.getValue());
            }

            MavenArtifactResolver resolver = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .build();
            appCoords.setPath(resolver.resolve(appMvnArtifact).getArtifact().getFile().toPath());

            try (CuratedApplication curatedApplication = QuarkusBootstrap.builder(appCoords.getPaths().getSinglePath())
                    .setProjectRoot(project.getBasedir().toPath())
                    .setBuildSystemProperties(realProperties)
                    .setAppArtifact(appCoords)
                    .setBaseName(finalName)
                    .setManagingProject(managingProject)
                    .setMavenArtifactResolver(resolver)
                    .setLocalProjectDiscovery(false)
                    .setBaseClassLoader(BuildMojo.class.getClassLoader())
                    .setTargetDirectory(buildDir.toPath())
                    .build().bootstrap()) {

                AugmentAction action = curatedApplication.createAugmentor();
                AugmentResult result = action.createProductionApplication();
            } finally {

                for (Map.Entry<String, String> e : old.entrySet()) {
                    if (e.getValue() == null) {
                        System.clearProperty(e.getKey());
                    } else {
                        System.setProperty(e.getKey(), e.getValue());
                    }
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate native image", e);
        }

    }

    private Map<String, String> createCustomConfig() {
        Map<String, String> configs = new HashMap<>();
        configs.put(QUARKUS_PACKAGE_TYPE, "native");
        if (addAllCharsets != null) {
            configs.put("quarkus.native.add-all-charsets", addAllCharsets.toString());
        }
        if (additionalBuildArgs != null && !additionalBuildArgs.isEmpty()) {
            configs.put("quarkus.native.additional-build-args",
                    additionalBuildArgs.stream()
                            .map(val -> val.replace("\\", "\\\\"))
                            .map(val -> val.replace(",", "\\,"))
                            .collect(joining(",")));
        }
        if (autoServiceLoaderRegistration != null) {
            configs.put("quarkus.native.auto-service-loader-registration", autoServiceLoaderRegistration.toString());
        }
        if (cleanupServer != null) {
            configs.put("quarkus.native.cleanup-server", cleanupServer.toString());
        }
        if (debugBuildProcess != null) {
            configs.put("quarkus.native.debug-build-process", debugBuildProcess.toString());
        }
        if (debugSymbols != null) {
            configs.put("quarkus.native.debug-symbols", debugSymbols.toString());
        }
        if (disableReports != null) {
            configs.put("quarkus.native.enable-reports", Boolean.toString(!disableReports));
        }
        if (enableReports != null) {
            configs.put("quarkus.native.enable-reports", enableReports.toString());
        }
        if (containerRuntime != null && !containerRuntime.trim().isEmpty()) {
            configs.put("quarkus.native.container-runtime", containerRuntime);
        } else if (dockerBuild != null && !dockerBuild.trim().isEmpty()) {
            if (!dockerBuild.toLowerCase().equals("false")) {
                if (dockerBuild.toLowerCase().equals("true")) {
                    configs.put("quarkus.native.container-runtime", "docker");
                } else {
                    configs.put("quarkus.native.container-runtime", dockerBuild);
                }
            }
        }
        if (containerRuntimeOptions != null && !containerRuntimeOptions.trim().isEmpty()) {
            configs.put("quarkus.native.container-runtime-options", containerRuntimeOptions);
        }
        if (dumpProxies != null) {
            configs.put("quarkus.native.dump-proxies", dumpProxies.toString());
        }
        if (enableAllSecurityServices != null) {
            configs.put("quarkus.native.enable-all-security-services", enableAllSecurityServices.toString());
        }
        if (enableFallbackImages != null) {
            configs.put("quarkus.native.enable-fallback-images", enableFallbackImages.toString());
        }
        if (enableHttpsUrlHandler != null) {
            configs.put("quarkus.native.enable-https-url-handler", enableHttpsUrlHandler.toString());
        }
        if (enableHttpUrlHandler != null) {
            configs.put("quarkus.native.enable-http-url-handler", enableHttpUrlHandler.toString());
        }
        if (enableIsolates != null) {
            configs.put("quarkus.native.enable-isolates", enableIsolates.toString());
        }
        if (Boolean.FALSE.equals(enableJni)) {
            getLog().warn("Your application is setting the deprecated 'enableJni' Maven option to false. Please"
                    + " consider removing this option as it is ignored (JNI is always enabled) and it will be removed"
                    + " in a future Quarkus version.");
        }

        if (enableServer != null) {
            configs.put("quarkus.native.enable-server", enableServer.toString());
        }

        if (enableVMInspection != null) {
            configs.put("quarkus.native.enable-vm-inspection", enableVMInspection.toString());
        }
        if (fullStackTraces != null) {
            configs.put("quarkus.native.full-stack-traces", fullStackTraces.toString());
        }
        if (graalvmHome != null && !graalvmHome.trim().isEmpty()) {
            configs.put("quarkus.native.graalvm-home", graalvmHome);
        }
        if (javaHome != null && !javaHome.toString().isEmpty()) {
            configs.put("quarkus.native.java-home", javaHome.toString());
        }
        if (nativeImageXmx != null && !nativeImageXmx.trim().isEmpty()) {
            configs.put("quarkus.native.native-image-xmx", nativeImageXmx);
        }
        if (reportErrorsAtRuntime != null) {
            configs.put("quarkus.native.report-errors-at-runtime", reportErrorsAtRuntime.toString());
        }
        if (reportExceptionStackTraces != null) {
            configs.put("quarkus.native.report-exception-stack-traces", reportExceptionStackTraces.toString());
        }
        if (publishDebugBuildProcessPort) {
            configs.put("quarkus.native.publish-debug-build-process-port",
                    Boolean.toString(publishDebugBuildProcessPort));
        }
        return configs;

    }

}
