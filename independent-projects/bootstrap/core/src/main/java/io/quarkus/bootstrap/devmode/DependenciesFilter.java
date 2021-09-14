package io.quarkus.bootstrap.devmode;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.jboss.logging.Logger;

public class DependenciesFilter {

    private static final Logger log = Logger.getLogger(DependenciesFilter.class);

    public static List<LocalProject> filterNotReloadableDependencies(LocalProject localProject,
            MavenArtifactResolver mvnResolver) throws BootstrapMavenException {
        final AppArtifact appArtifact = localProject.getAppArtifact("jar");
        final List<Artifact> projectDeps = new ArrayList<>();
        mvnResolver
                .resolveDependencies(
                        new DefaultArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(),
                                appArtifact.getClassifier(), appArtifact.getType(), appArtifact.getVersion()),
                        Collections.emptyList())
                .getRoot().accept(new TreeDependencyVisitor(new DependencyVisitor() {
                    @Override
                    public boolean visitEnter(DependencyNode node) {
                        final Dependency dep = node.getDependency();
                        if (dep != null && dep.getArtifact().getFile().isDirectory()) {
                            org.eclipse.aether.artifact.Artifact a = dep.getArtifact();
                            final org.apache.maven.artifact.DefaultArtifact mvnArtifact = new org.apache.maven.artifact.DefaultArtifact(
                                    a.getGroupId(), a.getArtifactId(), a.getVersion(), dep.getScope(), a.getExtension(),
                                    a.getClassifier(), new DefaultArtifactHandler("jar"));
                            mvnArtifact.setFile(a.getFile());
                            projectDeps.add(mvnArtifact);
                        }
                        return true;
                    }

                    @Override
                    public boolean visitLeave(DependencyNode node) {
                        return true;
                    }
                }));
        return filterNotReloadableDependencies(localProject, projectDeps, mvnResolver.getSystem(),
                mvnResolver.getSession(), mvnResolver.getRepositories());
    }

    public static List<LocalProject> filterNotReloadableDependencies(LocalProject localProject,
            Iterable<Artifact> projectDeps,
            RepositorySystem repoSystem,
            RepositorySystemSession repoSession,
            List<RemoteRepository> repos) {
        final LocalWorkspace workspace = localProject.getWorkspace();
        if (workspace == null) {
            return Collections.singletonList(localProject);
        }

        List<LocalProject> ret = new ArrayList<>();
        Set<AppArtifactKey> extensionsAndDeps = new HashSet<>();

        ret.add(localProject);
        for (Artifact a : projectDeps) {
            final AppArtifactKey depKey = new AppArtifactKey(a.getGroupId(), a.getArtifactId());
            final LocalProject project = workspace.getProject(depKey);
            if (project == null) {
                continue;
            }
            if ("test".equals(a.getScope()) || "pom".equals(a.getType())) {
                continue;
            }
            if (!project.getVersion().equals(a.getVersion())) {
                log.warn(depKey + " is excluded from live coding since the application depends on version "
                        + a.getVersion() + " while the version present in the workspace is " + project.getVersion());
                continue;
            }

            if (project.getClassesDir() != null &&
            //if this project also contains Quarkus extensions we do no want to include these in the discovery
            //a bit of an edge case, but if you try and include a sample project with your extension you will
            //run into problems without this
                    (Files.exists(project.getClassesDir().resolve("META-INF/quarkus-extension.properties")) ||
                            Files.exists(project.getClassesDir().resolve("META-INF/quarkus-build-steps.list")))) {
                // TODO add the deployment deps
                extensionDepWarning(depKey);
                try {
                    final DependencyNode depRoot = repoSystem.collectDependencies(repoSession, new CollectRequest()
                            .setRoot(new org.eclipse.aether.graph.Dependency(
                                    new DefaultArtifact(a.getGroupId(), a.getArtifactId(),
                                            a.getClassifier(), a.getArtifactHandler().getExtension(), a.getVersion()),
                                    JavaScopes.RUNTIME))
                            .setRepositories(repos)).getRoot();
                    depRoot.accept(new DependencyVisitor() {
                        @Override
                        public boolean visitEnter(DependencyNode node) {
                            final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
                            if ("jar".equals(artifact.getExtension())) {
                                extensionsAndDeps.add(new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId()));
                            }
                            return true;
                        }

                        @Override
                        public boolean visitLeave(DependencyNode node) {
                            return true;
                        }
                    });
                } catch (DependencyCollectionException e) {
                    throw new RuntimeException("Failed to collect dependencies for " + a, e);
                }
            } else {
                ret.add(project);
            }
        }

        if (extensionsAndDeps.isEmpty()) {
            return ret;
        }

        Iterator<LocalProject> iterator = ret.iterator();
        while (iterator.hasNext()) {
            final LocalProject localDep = iterator.next();
            if (extensionsAndDeps.contains(localDep.getKey())) {
                extensionDepWarning(localDep.getKey());
                iterator.remove();
            }
        }
        return ret;
    }

    private static void extensionDepWarning(AppArtifactKey key) {
        log.warn("Local Quarkus extension dependency " + key + " will not be hot-reloadable");
    }
}
