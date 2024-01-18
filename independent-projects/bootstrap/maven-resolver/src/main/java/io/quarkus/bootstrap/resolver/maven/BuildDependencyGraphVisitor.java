/**
 *
 */
package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.newDependencyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.DependencyFlags;

public class BuildDependencyGraphVisitor {

    private final MavenArtifactResolver resolver;
    private final ApplicationModelBuilder appBuilder;
    private final Consumer<String> buildTreeConsumer;
    private final List<Boolean> depth;

    private DependencyNode currentDeployment;
    private DependencyNode currentRuntime;
    private Artifact runtimeArtifactToFind;

    public BuildDependencyGraphVisitor(MavenArtifactResolver resolver, ApplicationModelBuilder appBuilder,
            Consumer<String> buildTreeConsumer) {
        this.resolver = resolver;
        this.appBuilder = appBuilder;
        this.buildTreeConsumer = buildTreeConsumer;
        if (buildTreeConsumer == null) {
            depth = null;
        } else {
            depth = new ArrayList<>();
        }
    }

    public void visit(DependencyNode node) throws BootstrapMavenException {
        if (depth != null) {
            consume(node);
        }
        final Dependency dep = node.getDependency();

        final DependencyNode previousDeployment = currentDeployment;
        final DependencyNode previousRuntime = currentRuntime;
        final Artifact previousRuntimeArtifact = runtimeArtifactToFind;

        final Artifact newRuntimeArtifact = ApplicationDependencyTreeResolver.getRuntimeArtifact(node);
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

        final int childrenTotal = children.size();
        if (childrenTotal > 0) {
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
        var buf = new StringBuilder();
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

    private void visitLeave(DependencyNode node) throws BootstrapMavenException {
        final Dependency dep = node.getDependency();
        if (dep == null) {
            return;
        }
        if (currentDeployment == null) {
            return;
        }
        if (currentRuntime == null && appBuilder.getDependency(getKey(node.getArtifact())) == null) {
            appBuilder.addDependency(newDependencyBuilder(node, resolver).setFlags(DependencyFlags.DEPLOYMENT_CP));
        } else if (currentRuntime == node) {
            currentRuntime = null;
            runtimeArtifactToFind = null;
        }
        if (currentDeployment == node) {
            currentDeployment = null;
        }
    }
}
