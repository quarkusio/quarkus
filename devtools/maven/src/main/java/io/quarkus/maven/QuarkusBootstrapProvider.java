package io.quarkus.maven;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.RemoteRepositoryManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.expression.Expression;

@Component(role = QuarkusBootstrapProvider.class, instantiationStrategy = "singleton")
public class QuarkusBootstrapProvider implements Closeable {

    @Requirement(role = RepositorySystem.class, optional = false)
    protected RepositorySystem repoSystem;

    @Requirement(role = RemoteRepositoryManager.class, optional = false)
    protected RemoteRepositoryManager remoteRepoManager;

    private final Cache<String, QuarkusAppBootstrapProvider> appBootstrapProviders = CacheBuilder.newBuilder()
            .concurrencyLevel(4).softValues().initialCapacity(10).build();

    static ArtifactKey getProjectId(MavenProject project) {
        return new GACT(project.getGroupId(), project.getArtifactId());
    }

    public RepositorySystem repositorySystem() {
        return repoSystem;
    }

    public RemoteRepositoryManager remoteRepositoryManager() {
        return remoteRepoManager;
    }

    private QuarkusAppBootstrapProvider provider(ArtifactKey projectId, String executionId) {
        try {
            return appBootstrapProviders.get(String.format("%s-%s", projectId, executionId), QuarkusAppBootstrapProvider::new);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to cache a new instance of " + QuarkusAppBootstrapProvider.class.getName(),
                    e);
        }
    }

    public CuratedApplication bootstrapApplication(QuarkusBootstrapMojo mojo, LaunchMode mode)
            throws MojoExecutionException {
        return provider(mojo.projectId(), mojo.executionId()).bootstrapApplication(mojo, mode);
    }

    public ApplicationModel getResolvedApplicationModel(ArtifactKey projectId, LaunchMode mode) {
        if (appBootstrapProviders.size() == 0) {
            return null;
        }
        final QuarkusAppBootstrapProvider provider = appBootstrapProviders.getIfPresent(projectId + "-null");
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
        for (QuarkusAppBootstrapProvider p : appBootstrapProviders.asMap().values()) {
            try {
                p.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class QuarkusAppBootstrapProvider implements Closeable {

        private CuratedApplication prodApp;
        private CuratedApplication devApp;
        private CuratedApplication testApp;

        private MavenArtifactResolver artifactResolver(QuarkusBootstrapMojo mojo, LaunchMode mode)
                throws MojoExecutionException {
            try {
                return MavenArtifactResolver.builder()
                        .setWorkspaceDiscovery(mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST)
                        .setPreferPomsFromWorkspace(mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST)
                        .setRepositorySystem(repoSystem)
                        .setRepositorySystemSession(mojo.repositorySystemSession())
                        .setRemoteRepositories(mojo.remoteRepositories())
                        .setRemoteRepositoryManager(remoteRepoManager)
                        .build();
            } catch (BootstrapMavenException e) {
                throw new MojoExecutionException("Failed to initialize Quarkus bootstrap Maven artifact resolver", e);
            }
        }

        protected CuratedApplication doBootstrap(QuarkusBootstrapMojo mojo, LaunchMode mode)
                throws MojoExecutionException {
            final Properties projectProperties = mojo.mavenProject().getProperties();
            final Properties effectiveProperties = new Properties();
            // quarkus. properties > ignoredEntries in pom.xml
            if (mojo.ignoredEntries() != null && mojo.ignoredEntries().length > 0) {
                String joinedEntries = String.join(",", mojo.ignoredEntries());
                effectiveProperties.setProperty("quarkus.package.user-configured-ignored-entries", joinedEntries);
            }
            for (String name : projectProperties.stringPropertyNames()) {
                if (name.startsWith("quarkus.")) {
                    effectiveProperties.setProperty(name, projectProperties.getProperty(name));
                }
            }

            // Add plugin properties
            effectiveProperties.putAll(mojo.properties());

            effectiveProperties.putIfAbsent("quarkus.application.name", mojo.mavenProject().getArtifactId());
            effectiveProperties.putIfAbsent("quarkus.application.version", mojo.mavenProject().getVersion());

            // Add other properties that may be required for expansion
            for (Object value : effectiveProperties.values()) {
                for (String reference : Expression.compile((String) value, LENIENT_SYNTAX, NO_TRIM).getReferencedStrings()) {
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

            final BootstrapAppModelResolver modelResolver = new BootstrapAppModelResolver(artifactResolver(mojo, mode))
                    .setDevMode(mode == LaunchMode.DEVELOPMENT)
                    .setTest(mode == LaunchMode.TEST);

            final List<MavenProject> localProjects = mojo.mavenProject().getCollectedProjects();
            final Set<ArtifactKey> reloadableModules = new HashSet<>(localProjects.size() + 1);
            final ArtifactCoords artifactCoords = appArtifact(mojo);
            reloadableModules.add(new GACT(artifactCoords.getGroupId(), artifactCoords.getArtifactId()));
            for (MavenProject project : localProjects) {
                reloadableModules.add(new GACT(project.getGroupId(), project.getArtifactId()));
            }

            final ApplicationModel appModel;
            try {
                appModel = modelResolver.resolveManagedModel(artifactCoords, Collections.emptyList(), managingProject(mojo),
                        reloadableModules);
            } catch (AppModelResolverException e) {
                throw new MojoExecutionException("Failed to bootstrap application in " + mode + " mode", e);
            }

            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setAppArtifact(appModel.getAppArtifact())
                    .setExistingModel(appModel)
                    .setIsolateDeployment(true)
                    .setBaseClassLoader(getClass().getClassLoader())
                    .setBuildSystemProperties(effectiveProperties)
                    .setProjectRoot(mojo.baseDir().toPath())
                    .setBaseName(mojo.finalName())
                    .setTargetDirectory(mojo.buildDir().toPath());

            try {
                return builder.build().bootstrap();
            } catch (BootstrapException e) {
                throw new MojoExecutionException("Failed to bootstrap the application", e);
            }
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

        protected GACTV managingProject(QuarkusBootstrapMojo mojo) {
            if (mojo.appArtifactCoords() == null) {
                return null;
            }
            final Artifact artifact = mojo.mavenProject().getArtifact();
            return new GACTV(artifact.getGroupId(), artifact.getArtifactId(),
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
            String classifier = "";
            String type = GACTV.TYPE_JAR;
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
            Path path = null;
            if (version == null) {
                for (Artifact dep : mojo.mavenProject().getArtifacts()) {
                    if (dep.getArtifactId().equals(artifactId)
                            && dep.getGroupId().equals(groupId)
                            && dep.getClassifier().equals(classifier)
                            && dep.getType().equals(type)) {
                        version = dep.getVersion();
                        if (dep.getFile() != null) {
                            path = dep.getFile().toPath();
                        }
                        break;
                    }
                }
                if (version == null) {
                    throw new IllegalStateException(
                            "Failed to locate " + appArtifactCoords + " among the project dependencies");
                }
            }

            return new GACTV(groupId, artifactId, classifier, type, version);
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
