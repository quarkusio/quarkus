package io.quarkus.bootstrap.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

public class DependencyUtils {

    public static ArtifactKey getKey(Artifact artifact) {
        return ArtifactKey.of(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension());
    }

    public static ArtifactCoords getCoords(Artifact artifact) {
        return new GACTV(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
    }

    public static Map<ArtifactKey, Dependency> toMap(List<Dependency> deps) {
        final Map<ArtifactKey, Dependency> map = new HashMap<>(deps.size());
        for (int i = 0; i < deps.size(); i++) {
            final Dependency dep = deps.get(i);
            map.put(getKey(dep.getArtifact()), dep);
        }
        return map;
    }

    public static void putAll(Map<ArtifactKey, Dependency> map, List<Dependency> deps) {
        for (int i = 0; i < deps.size(); i++) {
            final Dependency dep = deps.get(i);
            map.putIfAbsent(getKey(dep.getArtifact()), dep);
        }
    }

    public static List<Dependency> mergeDependencies(List<Dependency> dominant, List<Dependency> recessive,
            Map<ArtifactKey, Dependency> managedVersions, Set<String> excludedScopes) {
        if (dominant.isEmpty() && recessive.isEmpty()) {
            return List.of();
        }
        final List<Dependency> result = new ArrayList<>(dominant.size() + recessive.size());
        final Map<ArtifactKey, Dependency> dominantMap = new HashMap<>(dominant.size());
        for (Dependency dep : dominant) {
            if (!excludedScopes.contains(dep.getScope())) {
                result.add(dep);
                final ArtifactKey key = getKey(dep.getArtifact());
                if (!managedVersions.containsKey(key)) {
                    dominantMap.put(key, dep);
                }
            }
        }
        for (Dependency dep : recessive) {
            if (!excludedScopes.contains(dep.getScope())) {
                final ArtifactKey key = getKey(dep.getArtifact());
                if (!dominantMap.containsKey(key)) {
                    if (dep.getArtifact().getVersion() == null) {
                        final Dependency managed = managedVersions.get(key);
                        if (managed != null) {
                            dep = dep.setArtifact(dep.getArtifact().setVersion(managed.getArtifact().getVersion()));
                        }
                    }
                    result.add(dep);
                }
            }
        }
        return result;
    }

    public static Artifact toArtifact(String str) {
        final String groupId;
        final String artifactId;
        String classifier = ArtifactCoords.DEFAULT_CLASSIFIER;
        String type = ArtifactCoords.TYPE_JAR;
        String version = null;

        int offset = 0;
        int colon = str.indexOf(':', offset);
        final int length = str.length();
        if (colon < offset + 1 || colon == length - 1) {
            illegalDependencyFormat(str);
        }
        groupId = str.substring(offset, colon);
        offset = colon + 1;
        colon = str.indexOf(':', offset);
        if (colon < 0) {
            artifactId = str.substring(offset, length);
        } else {
            if (colon == length - 1) {
                illegalDependencyFormat(str);
            }
            artifactId = str.substring(offset, colon);
            offset = colon + 1;
            colon = str.indexOf(':', offset);
            if (colon < 0) {
                version = str.substring(offset, length);
            } else {
                if (colon == length - 1) {
                    illegalDependencyFormat(str);
                }
                type = str.substring(offset, colon);
                offset = colon + 1;
                colon = str.indexOf(':', offset);
                if (colon < 0) {
                    version = str.substring(offset, length);
                } else {
                    if (colon == length - 1) {
                        illegalDependencyFormat(str);
                    }
                    classifier = type;
                    type = str.substring(offset, colon);
                    version = str.substring(colon + 1);
                }
            }
        }
        return new DefaultArtifact(groupId, artifactId, classifier, type, version);
    }

    private static void illegalDependencyFormat(String str) {
        throw new IllegalArgumentException("Bad artifact coordinates " + str
                + ", expected format is <groupId>:<artifactId>[:<extension>|[:<classifier>:<extension>]]:<version>");
    }

    public static ResolvedDependencyBuilder newDependencyBuilder(DependencyNode node, MavenArtifactResolver resolver)
            throws BootstrapMavenException {
        var artifact = node.getDependency().getArtifact();
        if (artifact.getFile() == null) {
            artifact = resolver.resolve(artifact, node.getRepositories()).getArtifact();
        }
        int flags = 0;
        if (node.getDependency().isOptional()) {
            flags |= DependencyFlags.OPTIONAL;
        }
        WorkspaceModule module = null;
        if (resolver.getProjectModuleResolver() != null) {
            module = resolver.getProjectModuleResolver().getProjectModule(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getVersion());
            if (module != null) {
                flags |= DependencyFlags.WORKSPACE_MODULE;
            }
        }
        return toAppArtifact(artifact, module)
                .setScope(node.getDependency().getScope())
                .setFlags(flags);
    }

    public static ResolvedDependencyBuilder toAppArtifact(Artifact artifact, WorkspaceModule module) {
        return ResolvedDependencyBuilder.newInstance()
                .setWorkspaceModule(module)
                .setGroupId(artifact.getGroupId())
                .setArtifactId(artifact.getArtifactId())
                .setClassifier(artifact.getClassifier())
                .setType(artifact.getExtension())
                .setVersion(artifact.getVersion())
                .setResolvedPaths(artifact.getFile() == null ? PathList.empty() : PathList.of(artifact.getFile().toPath()));
    }

    public static boolean hasWinner(DependencyNode node) {
        return node.getData().containsKey(ConflictResolver.NODE_DATA_WINNER) && node.getChildren().isEmpty();
    }

    public static DependencyNode getWinner(DependencyNode node) {
        final DependencyNode winner = (DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        return winner == null || !node.getChildren().isEmpty() ? null : winner;
    }
}
