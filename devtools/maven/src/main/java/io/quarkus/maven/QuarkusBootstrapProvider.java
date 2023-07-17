package io.quarkus.maven;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
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
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.components.ManifestSection;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.expression.Expression;

@Singleton
@Named
public class QuarkusBootstrapProvider implements Closeable {

    private static final String MANIFEST_SECTIONS_PROPERTY_PREFIX = "quarkus.package.manifest.manifest-sections";
    private static final String MANIFEST_ATTRIBUTES_PROPERTY_PREFIX = "quarkus.package.manifest.attributes";

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

    static Map<Path, Model> getProjectMap(MavenSession session) {
        final List<MavenProject> allProjects = session.getAllProjects();
        if (allProjects == null) {
            return Map.of();
        }
        final Map<Path, Model> projectModels = new HashMap<>(allProjects.size());
        for (MavenProject mp : allProjects) {
            final Model model = mp.getOriginalModel();
            model.setPomFile(mp.getFile());
            // activated profiles or custom extensions may have overridden the build defaults
            model.setBuild(mp.getModel().getBuild());
            projectModels.put(mp.getBasedir().toPath(), model);
            // The Maven Model API determines the project directory as the directory containing the POM file.
            // However, in case when plugins manipulating POMs store their results elsewhere
            // (such as the flatten plugin storing the flattened POM under the target directory),
            // both the base directory and the directory containing the POM file should be added to the map.
            var pomDir = mp.getFile().getParentFile();
            if (!pomDir.equals(mp.getBasedir())) {
                projectModels.put(pomDir.toPath(), model);
            }
        }
        return projectModels;
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
        return bootstrapper(mojo).bootstrapApplication(mojo, mode);
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

        private CuratedApplication prodApp;
        private CuratedApplication devApp;
        private CuratedApplication testApp;

        private MavenArtifactResolver artifactResolver(QuarkusBootstrapMojo mojo, LaunchMode mode)
                throws MojoExecutionException {
            try {
                if (mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST || isWorkspaceDiscovery(mojo)) {
                    return workspaceProvider.createArtifactResolver(
                            BootstrapMavenContext.config()
                                    .setCurrentProject(mojo.mavenProject().getFile().toString())
                                    .setPreferPomsFromWorkspace(true)
                                    .setProjectModelProvider(getProjectMap(mojo.mavenSession())::get));
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
                throw new MojoExecutionException("Failed to initialize Quarkus bootstrap Maven artifact resolver", e);
            }
        }

        private CuratedApplication doBootstrap(QuarkusBootstrapMojo mojo, LaunchMode mode)
                throws MojoExecutionException {
            final BootstrapAppModelResolver modelResolver = new BootstrapAppModelResolver(artifactResolver(mojo, mode))
                    .setDevMode(mode == LaunchMode.DEVELOPMENT)
                    .setTest(mode == LaunchMode.TEST)
                    .setCollectReloadableDependencies(mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST);

            final ArtifactCoords appArtifact = appArtifact(mojo);
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
                    .setForcedDependencies(forcedDependencies);

            try {
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
                effectiveProperties.setProperty("quarkus.package.user-configured-ignored-entries", joinedEntries);
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

        protected CuratedApplication bootstrapApplication(QuarkusBootstrapMojo mojo, LaunchMode mode)
                throws MojoExecutionException {
            if (mode == LaunchMode.DEVELOPMENT) {
                return devApp == null ? devApp = doBootstrap(mojo, mode) : devApp;
            }
            if (mode == LaunchMode.TEST) {
                return testApp == null ? testApp = doBootstrap(mojo, mode) : testApp;
            }
            return prodApp == null ? prodApp = doBootstrap(mojo, mode) : prodApp;
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

        private ArtifactCoords appArtifact(QuarkusBootstrapMojo mojo)
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
                return new ResolvedArtifactDependency(projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                        projectArtifact.getClassifier(), projectArtifact.getArtifactHandler().getExtension(),
                        projectArtifact.getVersion(), projectFile.toPath());
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

            return ArtifactCoords.of(groupId, artifactId, classifier, type, version);
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
