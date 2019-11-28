/**
 *
 */
package io.quarkus.bootstrap.resolver.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;

import io.quarkus.bootstrap.model.AppArtifactKey;

public class BuildDependencyGraphVisitor {

    private final Set<AppArtifactKey> appDeps;
    private final StringBuilder buf;
    private final Consumer<String> buildTreeConsumer;
    private final List<Boolean> depth;

    private DependencyNode deploymentNode;
    private DependencyNode runtimeNode;
    private Artifact runtimeArtifact;

    /**
     * Nodes that are only present in the deployment class loader
     */
    private final List<DependencyNode> deploymentDepNodes = new ArrayList<>();
    private final List<ArtifactRequest> requests = new ArrayList<>();


    public BuildDependencyGraphVisitor(Set<AppArtifactKey> appDeps, Consumer<String> buildTreeConsumer) {
        this.appDeps = appDeps;
        this.buildTreeConsumer = buildTreeConsumer;
        if(buildTreeConsumer == null) {
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
        if(depth != null) {
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
            if(!depth.isEmpty()) {
                buf.append(" (").append(node.getDependency().getScope());
                if(node.getDependency().isOptional()) {
                    buf.append(" optional");
                }
                buf.append(')');
            }
            buildTreeConsumer.accept(buf.toString());
        }
        visitEnter(node);
        final List<DependencyNode> children = node.getChildren();
        if(!children.isEmpty()) {
            final int childrenTotal = children.size();
            if(childrenTotal == 1) {
                if(depth != null) {
                    depth.add(false);
                }
                visit(children.get(0));
            } else {
                if(depth != null) {
                    depth.add(true);
                }
                int i = 0;
                while(true) {
                    visit(children.get(i++));
                    if(i < childrenTotal - 1) {
                        continue;
                    } else if(i == childrenTotal) {
                        break;
                    } else if(depth != null) {
                        depth.set(depth.size() - 1, false);
                    }
                }
            }
            if(depth != null) {
                depth.remove(depth.size() - 1);
            }
        }
        visitLeave(node);
    }

    private void visitEnter(DependencyNode node) {
        final Dependency dep = node.getDependency();
        if (deploymentNode == null) {
            runtimeArtifact = DeploymentInjectingDependencyVisitor.getRuntimeArtifact(node);
            if (runtimeArtifact != null) {
                deploymentNode = node;
            }
        } else if (runtimeArtifact != null && runtimeNode == null && runtimeArtifact.equals(dep.getArtifact())) {
            runtimeNode = node;
        }
    }

    private void visitLeave(DependencyNode node) {
        final Dependency dep = node.getDependency();
        if(dep == null) {
            return;
        }
        final Artifact artifact = dep.getArtifact();
        if (artifact.getFile() == null) {
            requests.add(new ArtifactRequest(node));
        }
        if (deploymentNode != null) {
            if (runtimeNode == null && !appDeps.contains(new AppArtifactKey(artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension()))) {
                deploymentDepNodes.add(node);
            } else if (runtimeNode == node) {
                runtimeNode = null;
                runtimeArtifact = null;
            }
            if (deploymentNode == node) {
                deploymentNode = null;
            }
        }
    }
}
