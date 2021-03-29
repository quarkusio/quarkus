package io.quarkus.bootstrap.util;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.io.PrintWriter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;

public class DependencyNodeUtils {

    public static AppArtifactKey toKey(Artifact artifact) {
        return new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getExtension());
    }

    public static Artifact toArtifact(String str) {
        return toArtifact(str, 0);
    }

    private static Artifact toArtifact(String str, int offset) {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = "jar";
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
