/**
 *
 */
package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;

public class BuildDependencyGraphVisitor {

    private final Set<ArtifactKey> allRuntimeDeps;
    private final StringBuilder buf;
    private final Consumer<String> buildTreeConsumer;
    private final List<Boolean> depth;

    private DependencyNode currentDeployment;
    private DependencyNode currentRuntime;
    private Artifact runtimeArtifactToFind;

    /**
     * Nodes that are only present in the deployment class loader
     */
    private final List<DependencyNode> deploymentDepNodes = new ArrayList<>();
    private final List<ArtifactRequest> requests = new ArrayList<>();

    public BuildDependencyGraphVisitor(Set<ArtifactKey> allRuntimeDeps, Consumer<String> buildTreeConsumer) {
        this.allRuntimeDeps = allRuntimeDeps;
        this.buildTreeConsumer = buildTreeConsumer;
        if (buildTreeConsumer == null) {
            buf = null;
            depth = null;
        } else {
            buf = new StringBuilder();
            depth = new ArrayList<>();
        }
    }

    public List<DependencyNode> getDeploymentNodes() {
        return deploymentDepNodes;
    }

    public List<ArtifactRequest> getArtifactRequests() {
        return requests;
    }

    public void visit(DependencyNode node) {
        if (depth != null) {
            consume(node);
        }
        final Dependency dep = node.getDependency();

        final DependencyNode previousDeployment = currentDeployment;
        final DependencyNode previousRuntime = currentRuntime;
        final Artifact previousRuntimeArtifact = runtimeArtifactToFind;

        final Artifact newRuntimeArtifact = DeploymentInjectingDependencyVisitor.getRuntimeArtifact(node);
        if (newRuntimeArtifact != null) {
            currentDeployment = node;
            runtimeArtifactToFind = newRuntimeArtifact;
            currentRuntime = null;
        } else if (runtimeArtifactToFind != null && currentRuntime == null
                && runtimeArtifactToFind.equals(dep.getArtifact())) {
            currentRuntime = node;
            runtimeArtifactToFind = null;
        }

        final List<DependencyNode> children = node.getChildren();
        if (!children.isEmpty()) {
            final int childrenTotal = children.size();
            if (childrenTotal == 1) {
                if (depth != null) {
                    depth.add(false);
                }
                visit(children.get(0));
            } else {
                if (depth != null) {
                    depth.add(true);
                }
                int i = 0;
                while (i < childrenTotal) {
                    visit(children.get(i++));
                    if (depth != null && i == childrenTotal - 1) {
                        depth.set(depth.size() - 1, false);
                    }
                }
            }
            if (depth != null) {
                depth.remove(depth.size() - 1);
            }
        }
        visitLeave(node);

        currentDeployment = previousDeployment;
        currentRuntime = previousRuntime;
        runtimeArtifactToFind = previousRuntimeArtifact;
    }

    private void consume(DependencyNode node) {
        buf.setLength(0);
        if (!depth.isEmpty()) {
            for (int i = 0; i < depth.size() - 1; ++i) {
                if (depth.get(i)) {
                    //buf.append("|  ");
                    buf.append('\u2502').append("  ");
                } else {
                    buf.append("   ");
                }
            }
            if (depth.get(depth.size() - 1)) {
                //buf.append("|- ");
                buf.append('\u251c').append('\u2500').append(' ');
            } else {
                //buf.append("\\- ");
                buf.append('\u2514').append('\u2500').append(' ');
            }
        }
        buf.append(node.getArtifact());
        if (!depth.isEmpty()) {
            buf.append(" (").append(node.getDependency().getScope());
            if (node.getDependency().isOptional()) {
                buf.append(" optional");
            }
            buf.append(')');
        }
        buildTreeConsumer.accept(buf.toString());
    }

    private void visitLeave(DependencyNode node) {
        final Dependency dep = node.getDependency();
        if (dep == null) {
            return;
        }
        final Artifact artifact = dep.getArtifact();
        if (artifact.getFile() == null) {
            requests.add(new ArtifactRequest(node));
        }
        if (currentDeployment != null) {
            if (currentRuntime == null && !allRuntimeDeps.contains(new GACT(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getClassifier(), artifact.getExtension()))) {
                deploymentDepNodes.add(node);
            } else if (currentRuntime == node) {
                currentRuntime = null;
                runtimeArtifactToFind = null;
            }
            if (currentDeployment == node) {
                currentDeployment = null;
            }
        }
    }
}
