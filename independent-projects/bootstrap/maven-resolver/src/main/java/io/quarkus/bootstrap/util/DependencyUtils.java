package io.quarkus.bootstrap.util;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

public class DependencyUtils {

    public static ArtifactKey getKey(Artifact artifact) {
        return ArtifactKey.gact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension());
    }

    public static ArtifactCoords getCoords(Artifact artifact) {
        return new GACTV(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension(), artifact.getVersion());
    }

    public static List<Dependency> mergeDeps(List<Dependency> dominant, List<Dependency> recessive,
            Map<ArtifactKey, String> managedVersions, Set<String> excludedScopes) {
        final int initialCapacity = dominant.size() + recessive.size();
        if (initialCapacity == 0) {
            return List.of();
        }
        final List<Dependency> result = new ArrayList<>(initialCapacity);
        final Set<ArtifactKey> ids = new HashSet<>(initialCapacity, 1.0f);
        for (Dependency dependency : dominant) {
            if (excludedScopes.contains(dependency.getScope())) {
                continue;
            }
            ids.add(getKey(dependency.getArtifact()));
            result.add(dependency);
        }
        for (Dependency dependency : recessive) {
            if (excludedScopes.contains(dependency.getScope())) {
                continue;
            }
            final ArtifactKey id = getKey(dependency.getArtifact());
            if (ids.contains(id)) {
                continue;
            }
            final String managedVersion = managedVersions.get(id);
            if (managedVersion != null) {
                dependency = dependency.setArtifact(dependency.getArtifact().setVersion(managedVersion));
            }
            result.add(dependency);
        }
        return result;
    }

    public static Artifact toArtifact(String str) {
        return toArtifact(str, 0);
    }

    private static Artifact toArtifact(String str, int offset) {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = ArtifactCoords.TYPE_JAR;
        String version = null;

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

    public static void printTree(DependencyNode node) {
        PrintWriter out = new PrintWriter(System.out);
        try {
            printTree(node, out);
        } finally {
            out.flush();
        }
    }

    public static void printTree(DependencyNode node, PrintWriter out) {
        out.println("Dependency tree for " + node.getArtifact());
        printTree(node, 0, out);
    }

    private static void printTree(DependencyNode node, int depth, PrintWriter out) {
        if (node.getArtifact() != null) {
            for (int i = 0; i < depth; ++i) {
                out.append("  ");
            }
            out.println(node.getArtifact());
        }
        for (DependencyNode c : node.getChildren()) {
            printTree(c, depth + 1, out);
        }
    }
}
