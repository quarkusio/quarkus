package io.quarkus.maven;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.RemoteRepositoryManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.DependencyInfoProvider;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.EffectiveModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.maven.components.ManifestSection;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.expression.Expression;

@Singleton
@Named
public class QuarkusBootstrapProvider implements Closeable {

    private static final String MANIFEST_SECTIONS_PROPERTY_PREFIX = "quarkus.package.jar.manifest.sections";
    private static final String MANIFEST_ATTRIBUTES_PROPERTY_PREFIX = "quarkus.package.jar.manifest.attributes";

    private final QuarkusWorkspaceProvider workspaceProvider;
    private final RepositorySystem repoSystem;
    private final RemoteRepositoryManager remoteRepoManager;

    private final Cache<String, QuarkusMavenAppBootstrap> appBootstrapProviders = CacheBuilder.newBuilder()
            .concurrencyLevel(4).softValues().initialCapacity(10).build();

    @Inject
    public QuarkusBootstrapProvider(RepositorySystem repoSystem, RemoteRepositoryManager remoteRepoManager,
            QuarkusWorkspaceProvider workspaceProvider) {
        this.repoSystem = repoSystem;
        this.remoteRepoManager = remoteRepoManager;
        this.workspaceProvider = workspaceProvider;
    }

    static ArtifactKey getProjectId(MavenProject project) {
        return ArtifactKey.ga(project.getGroupId(), project.getArtifactId());
    }

    static void setProjectModels(QuarkusBootstrapMojo mojo, BootstrapMavenContextConfig<?> config) {
        final List<MavenProject> allProjects = mojo.mavenSession().getAllProjects();
        if (allProjects != null) {
            for (MavenProject mp : allProjects) {
                if (mojo.reloadPoms.contains(mp.getFile())) {
                    continue;
                }
                final Model model = getRawModel(mp);
                config.addProvidedModule(mp.getFile().toPath(), model, mp.getModel());
                // The Maven Model API determines the project directory as the directory containing the POM file.
                // However, in case when plugins manipulating POMs store their results elsewhere
                // (such as the flatten plugin storing the flattened POM under the target directory),
                // both the base directory and the directory containing the POM file should be added to the map.
                var pomDir = mp.getFile().getParentFile();
                if (!pomDir.equals(mp.getBasedir())) {
                    config.addProvidedModule(mp.getBasedir().toPath().resolve("pom.xml"), model, mp.getModel());
                }
            }
        }
    }

    /**
     * This method is meant to return the "raw" model, i.e. the one that would be obtained
     * by reading a {@code pom.xml} file, w/o interpolation, flattening, etc.
     * However, plugins, such as, {@code flatten-maven-plugin}, may manipulate raw POMs
     * early enough by stripping dependency management, test scoped dependencies, etc,
     * to break our bootstrap. So this method attempts to make sure the essential configuration
     * is still available to bootstrap a Quarkus app.
     *
     * @param mp Maven project
     * @return raw POM
     */
    private static Model getRawModel(MavenProject mp) {
        final Model model = mp.getOriginalModel();
        if (model.getDependencyManagement() == null) {
            // this could be the flatten plugin removing the dependencyManagement
            // in which case we set the effective dependency management to not lose the platform info
            model.setDependencyManagement(mp.getDependencyManagement());
            // it also helps to set the effective dependencies in this case
            // since the flatten plugin may remove the test dependencies from the POM
            model.setDependencies(mp.getDependencies());
        }
        model.setPomFile(mp.getFile());
        return model;
    }

    private static String getBootstrapProviderId(ArtifactKey moduleKey, String bootstrapId) {
        return bootstrapId == null ? moduleKey.toGacString() : moduleKey.toGacString() + "-" + bootstrapId;
    }

    @Deprecated(forRemoval = true)
    public RepositorySystem repositorySystem() {
        return workspaceProvider.getRepositorySystem();
    }

    @Deprecated(forRemoval = true)
    public RemoteRepositoryManager remoteRepositoryManager() {
        return remoteRepoManager;
    }

    public QuarkusMavenAppBootstrap bootstrapper(QuarkusBootstrapMojo mojo) {
        try {
            return appBootstrapProviders.get(getBootstrapProviderId(mojo.projectId(), mojo.bootstrapId()),
                    QuarkusMavenAppBootstrap::new);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to cache a new instance of " + QuarkusMavenAppBootstrap.class.getName(),
                    e);
        }
    }

    public CuratedApplication bootstrapApplication(QuarkusBootstrapMojo mojo, LaunchMode mode)
            throws MojoExecutionException {
        return bootstrapApplication(mojo, mode, null);
    }

    public CuratedApplication bootstrapApplication(QuarkusBootstrapMojo mojo, LaunchMode mode,
            Consumer<QuarkusBootstrap.Builder> builderCustomizer) throws MojoExecutionException {
        return bootstrapper(mojo).bootstrapApplication(mojo, mode, builderCustomizer);
    }

    public void closeApplication(QuarkusBootstrapMojo mojo, LaunchMode mode) {
        bootstrapper(mojo).closeApplication(mode);
    }

    /**
     * Workspace ID associated with a given bootstrap mojo.
     * If the returned value is {@code 0}, a workspace was not associated with the bootstrap mojo.
     *
     * @param mojo bootstrap mojo
     * @return workspace ID associated with a given bootstrap mojo
     */
    public int getWorkspaceId(QuarkusBootstrapMojo mojo) {
        return bootstrapper(mojo).workspaceId;
    }

    public ApplicationModel getResolvedApplicationModel(ArtifactKey projectId, LaunchMode mode, String bootstrapId) {
        if (appBootstrapProviders.size() == 0) {
            return null;
        }
        final QuarkusMavenAppBootstrap provider = appBootstrapProviders
                .getIfPresent(getBootstrapProviderId(projectId, bootstrapId));
        if (provider == null) {
            return null;
        }
        if (mode == LaunchMode.DEVELOPMENT) {
            return provider.devApp == null ? null : provider.devApp.getApplicationModel();
        }
        if (mode == LaunchMode.TEST) {
            return provider.testApp == null ? null : provider.testApp.getApplicationModel();
        }
        return provider.prodApp == null ? null : provider.prodApp.getApplicationModel();
    }

    @Override
    public void close() throws IOException {
        if (appBootstrapProviders.size() == 0) {
            return;
        }
        for (QuarkusMavenAppBootstrap p : appBootstrapProviders.asMap().values()) {
            try {
                p.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isWorkspaceDiscovery(QuarkusBootstrapMojo mojo) {
        String v = System.getProperty(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY);
        if (v == null) {
            v = mojo.mavenProject().getProperties().getProperty(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY);
        }
        return Boolean.parseBoolean(v);
    }

    public class QuarkusMavenAppBootstrap implements Closeable {

        private int workspaceId;
        private CuratedApplication prodApp;
        private CuratedApplication devApp;
        private CuratedApplication testApp;

        private MavenArtifactResolver artifactResolver(QuarkusBootstrapMojo mojo, LaunchMode mode) {
            try {
                if (mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST || isWorkspaceDiscovery(mojo)) {
                    final BootstrapMavenContextConfig<?> config = BootstrapMavenContext.config()
                            // it's important to pass user settings in case the process was not launched using the original mvn script,
                            // for example, using org.codehaus.plexus.classworlds.launcher.Launcher
                            .setUserSettings(mojo.mavenSession().getRequest().getUserSettingsFile())
                            .setCurrentProject(mojo.mavenProject().getFile().toString())
                            .setPreferPomsFromWorkspace(true)
                            // pass the repositories since Maven extensions could manipulate repository configs
                            .setRemoteRepositories(mojo.remoteRepositories())
                            .setEffectiveModelBuilder(BootstrapMavenContextConfig
                                    .getEffectiveModelBuilderProperty(mojo.mavenProject().getProperties()));
                    setProjectModels(mojo, config);
                    var resolver = workspaceProvider.createArtifactResolver(config);
                    final LocalProject currentProject = resolver.getMavenContext().getCurrentProject();
                    if (currentProject != null && workspaceId == 0) {
                        workspaceId = currentProject.getWorkspace().getId();
                    }
                    return resolver;
                }
                // PROD packaging mode with workspace discovery disabled
                return MavenArtifactResolver.builder()
                        .setWorkspaceDiscovery(false)
                        .setRepositorySystem(repoSystem)
                        .setRepositorySystemSession(mojo.repositorySystemSession())
                        .setRemoteRepositories(mojo.remoteRepositories())
                        .setRemoteRepositoryManager(remoteRepoManager)
                        .build();
            } catch (BootstrapMavenException e) {
                throw new RuntimeException("Failed to initialize Quarkus bootstrap Maven artifact resolver", e);
            }
        }

        private CuratedApplication doBootstrap(QuarkusBootstrapMojo mojo, LaunchMode mode,
                Consumer<QuarkusBootstrap.Builder> builderCustomizer) throws MojoExecutionException {

            final BootstrapAppModelResolver modelResolver = new BootstrapAppModelResolver(artifactResolver(mojo, mode))
                    .setLegacyModelResolver(
                            BootstrapAppModelResolver.isLegacyModelResolver(mojo.mavenProject().getProperties()))
                    .setDevMode(mode == LaunchMode.DEVELOPMENT)
                    .setTest(mode == LaunchMode.TEST)
                    .setCollectReloadableDependencies(mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST);

            final ResolvedDependencyBuilder appArtifact = getApplicationArtifactBuilder(mojo);
            Set<ArtifactKey> reloadableModules = Set.of();
            if (mode == LaunchMode.NORMAL) {
                // collect reloadable artifacts for remote-dev
                final List<MavenProject> localProjects = mojo.mavenProject().getCollectedProjects();
                final Set<ArtifactKey> localProjectKeys = new HashSet<>(localProjects.size());
                for (MavenProject p : localProjects) {
                    localProjectKeys.add(ArtifactKey.ga(p.getGroupId(), p.getArtifactId()));
                }
                reloadableModules = new HashSet<>(localProjects.size() + 1);
                for (Artifact a : mojo.mavenProject().getArtifacts()) {
                    if (localProjectKeys.contains(ArtifactKey.ga(a.getGroupId(), a.getArtifactId()))) {
                        reloadableModules
                                .add(ArtifactKey.of(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getType()));
                    }
                }
                reloadableModules.add(appArtifact.getKey());
            }

            final List<Dependency> forcedDependencies = mojo.forcedDependencies(mode);
            final ApplicationModel appModel;
            try {
                appModel = modelResolver.resolveManagedModel(appArtifact, forcedDependencies, managingProject(mojo),
                        reloadableModules);
            } catch (AppModelResolverException e) {
                throw new MojoExecutionException("Failed to bootstrap application in " + mode + " mode", e);
            }
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setAppArtifact(appModel.getAppArtifact())
                    .setExistingModel(appModel)
                    .setIsolateDeployment(true)
                    .setBaseClassLoader(getClass().getClassLoader())
                    .setBuildSystemProperties(getBuildSystemProperties(mojo, true))
                    .setProjectRoot(mojo.baseDir().toPath())
                    .setBaseName(mojo.finalName())
                    .setOriginalBaseName(mojo.mavenProject().getBuild().getFinalName())
                    .setTargetDirectory(mojo.buildDir().toPath())
                    .setForcedDependencies(forcedDependencies)
                    .setDependencyInfoProvider(() -> DependencyInfoProvider.builder()
                            .setMavenModelResolver(EffectiveModelResolver.of(artifactResolver(mojo, mode)))
                            .build());

            try {
                if (builderCustomizer != null) {
                    builderCustomizer.accept(builder);
                }
                return builder.build().bootstrap();
            } catch (BootstrapException e) {
                throw new MojoExecutionException("Failed to bootstrap the application", e);
            }
        }

        /**
         * Collects properties from a project configuration that are relevant for the build.
         * The {@code quarkusOnly} argument indicates whether only {@code quarkus.*} properties
         * should be collected, which is currently set to {@code true} for building an application.
         * {@code quarkusOnly} is set to {@code false} when initializing configuration for
         * source code generators, for example to enable {@code avro.*} properties, etc.
         *
         * @param mojo Mojo for which the properties should be collected
         * @param quarkusOnly whether to collect only 'quarkus.*' properties
         * @return properties from a project configuration that are relevant for the build
         * @throws MojoExecutionException in case of a failure
         */
        public Properties getBuildSystemProperties(QuarkusBootstrapMojo mojo, boolean quarkusOnly)
                throws MojoExecutionException {
            final Properties effectiveProperties = new Properties();
            // quarkus. properties > ignoredEntries in pom.xml
            if (mojo.ignoredEntries() != null && mojo.ignoredEntries().length > 0) {
                String joinedEntries = String.join(",", mojo.ignoredEntries());
                effectiveProperties.setProperty("quarkus.package.jar.user-configured-ignored-entries", joinedEntries);
            }

            final Properties projectProperties = mojo.mavenProject().getProperties();
            for (String name : projectProperties.stringPropertyNames()) {
                if (!quarkusOnly || name.startsWith("quarkus.")) {
                    effectiveProperties.setProperty(name, projectProperties.getProperty(name));
                }
            }

            // Add plugin properties
            effectiveProperties.putAll(mojo.properties());

            effectiveProperties.putIfAbsent("quarkus.application.name", mojo.mavenProject().getArtifactId());
            effectiveProperties.putIfAbsent("quarkus.application.version", mojo.mavenProject().getVersion());

            for (Map.Entry<String, String> attribute : mojo.manifestEntries().entrySet()) {
                if (attribute.getValue() == null) {
                    mojo.getLog().warn("Skipping manifest entry property " + attribute.getKey() + " with a missing value");
                } else {
                    effectiveProperties.put(toManifestAttributeKey(attribute.getKey()), attribute.getValue());
                }
            }
            for (ManifestSection section : mojo.manifestSections()) {
                for (Map.Entry<String, String> attribute : section.getManifestEntries().entrySet()) {
                    effectiveProperties
                            .put(toManifestSectionAttributeKey(section.getName(), attribute.getKey()),
                                    attribute.getValue());
                }
            }

            // Add other properties that may be required for expansion
            for (Object value : effectiveProperties.values()) {
                for (String reference : Expression.compile((String) value, LENIENT_SYNTAX, NO_TRIM)
                        .getReferencedStrings()) {
                    String referenceValue = mojo.mavenSession().getUserProperties().getProperty(reference);
                    if (referenceValue != null) {
                        effectiveProperties.setProperty(reference, referenceValue);
                        continue;
                    }

                    referenceValue = projectProperties.getProperty(reference);
                    if (referenceValue != null) {
                        effectiveProperties.setProperty(reference, referenceValue);
                    }
                }
            }
            return effectiveProperties;
        }

        private String toManifestAttributeKey(String key) throws MojoExecutionException {
            if (key.contains("\"")) {
                throw new MojoExecutionException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
            }
            return String.format("%s.\"%s\"", MANIFEST_ATTRIBUTES_PROPERTY_PREFIX, key);
        }

        private String toManifestSectionAttributeKey(String section, String key) throws MojoExecutionException {
            if (section.contains("\"")) {
                throw new MojoExecutionException(
                        "Manifest section name " + section + " is invalid. \" characters are not allowed.");
            }
            if (key.contains("\"")) {
                throw new MojoExecutionException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
            }
            return String.format("%s.\"%s\".\"%s\"", MANIFEST_SECTIONS_PROPERTY_PREFIX, section,
                    key);
        }

        protected CuratedApplication bootstrapApplication(QuarkusBootstrapMojo mojo, LaunchMode mode,
                Consumer<QuarkusBootstrap.Builder> builderCustomizer) throws MojoExecutionException {
            if (mode == LaunchMode.DEVELOPMENT) {
                return devApp == null ? devApp = doBootstrap(mojo, mode, builderCustomizer) : devApp;
            }
            if (mode == LaunchMode.TEST) {
                return testApp == null ? testApp = doBootstrap(mojo, mode, builderCustomizer) : testApp;
            }
            return prodApp == null ? prodApp = doBootstrap(mojo, mode, builderCustomizer) : prodApp;
        }

        protected void closeApplication(LaunchMode mode) {
            if (mode == LaunchMode.DEVELOPMENT) {
                if (devApp != null) {
                    devApp.close();
                    devApp = null;
                }
            } else if (mode == LaunchMode.TEST) {
                if (testApp != null) {
                    testApp.close();
                    testApp = null;
                }
            } else if (prodApp != null) {
                prodApp.close();
                prodApp = null;
            }
        }

        protected ArtifactCoords managingProject(QuarkusBootstrapMojo mojo) {
            if (mojo.appArtifactCoords() == null) {
                return null;
            }
            final Artifact artifact = mojo.mavenProject().getArtifact();
            return ArtifactCoords.of(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getClassifier(), artifact.getArtifactHandler().getExtension(),
                    artifact.getVersion());
        }

        private ResolvedDependencyBuilder getApplicationArtifactBuilder(QuarkusBootstrapMojo mojo)
                throws MojoExecutionException {
            String appArtifactCoords = mojo.appArtifactCoords();
            if (appArtifactCoords == null) {
                final Artifact projectArtifact = mojo.mavenProject().getArtifact();

                File projectFile = projectArtifact.getFile();
                if (projectFile == null) {
                    projectFile = new File(mojo.mavenProject().getBuild().getOutputDirectory());
                    if (!projectFile.exists()) {
                        /*
                         * TODO GenerateCodeMojo would fail
                         * if (hasSources(project)) {
                         * throw new MojoExecutionException("Project " + project.getArtifact() + " has not been compiled yet");
                         * }
                         */
                        if (!projectFile.mkdirs()) {
                            throw new MojoExecutionException("Failed to create the output dir " + projectFile);
                        }
                    }
                }
                return ResolvedDependencyBuilder.newInstance()
                        .setGroupId(projectArtifact.getGroupId())
                        .setArtifactId(projectArtifact.getArtifactId())
                        .setClassifier(projectArtifact.getClassifier())
                        .setType(projectArtifact.getArtifactHandler().getExtension())
                        .setVersion(projectArtifact.getVersion())
                        .setResolvedPath(projectFile.toPath());
            }

            final String[] coordsArr = appArtifactCoords.split(":");
            if (coordsArr.length < 2 || coordsArr.length > 5) {
                throw new MojoExecutionException(
                        "appArtifact expression " + appArtifactCoords
                                + " does not follow format groupId:artifactId:classifier:type:version");
            }
            final String groupId = coordsArr[0];
            final String artifactId = coordsArr[1];
            String classifier = ArtifactCoords.DEFAULT_CLASSIFIER;
            String type = ArtifactCoords.TYPE_JAR;
            String version = null;
            if (coordsArr.length == 3) {
                version = coordsArr[2];
            } else if (coordsArr.length > 3) {
                classifier = coordsArr[2] == null ? ArtifactCoords.DEFAULT_CLASSIFIER : coordsArr[2];
                type = coordsArr[3] == null ? ArtifactCoords.TYPE_JAR : coordsArr[3];
                if (coordsArr.length > 4) {
                    version = coordsArr[4];
                }
            }
            if (version == null) {
                for (Artifact dep : mojo.mavenProject().getArtifacts()) {
                    if (dep.getArtifactId().equals(artifactId)
                            && dep.getGroupId().equals(groupId)
                            && dep.getClassifier().equals(classifier)
                            && dep.getType().equals(type)) {
                        version = dep.getVersion();
                        break;
                    }
                }
                if (version == null) {
                    throw new IllegalStateException(
                            "Failed to locate " + appArtifactCoords + " among the project dependencies");
                }
            }

            return ResolvedDependencyBuilder.newInstance()
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setClassifier(classifier)
                    .setType(type)
                    .setVersion(version);
        }

        @Override
        public void close() {
            if (prodApp != null) {
                prodApp.close();
                prodApp = null;
            }
            if (devApp != null) {
                devApp.close();
                devApp = null;
            }
            if (testApp != null) {
                testApp.close();
                testApp = null;
            }
        }
    }
}
