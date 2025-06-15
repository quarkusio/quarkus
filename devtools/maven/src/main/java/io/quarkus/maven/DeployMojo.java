package io.quarkus.maven;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.cmd.DeployCommandActionResultBuildItem;
import io.quarkus.deployment.cmd.DeployCommandDeclarationHandler;
import io.quarkus.deployment.cmd.DeployCommandDeclarationResultBuildItem;
import io.quarkus.deployment.cmd.DeployCommandHandler;

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class DeployMojo extends AbstractDeploymentMojo {

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        return super.beforeExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException {
        try (CuratedApplication curatedApplication = bootstrapApplication()) {
            // get list of extensions that support quarkus:deploy
            AtomicReference<List<String>> tooMany = new AtomicReference<>();
            AugmentAction action = curatedApplication.createAugmentor();
            action.performCustomBuild(DeployCommandDeclarationHandler.class.getName(), new Consumer<List<String>>() {
                @Override
                public void accept(List<String> strings) {
                    tooMany.set(strings);
                }
            }, DeployCommandDeclarationResultBuildItem.class.getName());
            String target = System.getProperty("quarkus.deploy.target");
            List<String> targets = tooMany.get();
            if (targets.isEmpty() && target == null) {
                // weave in kubernetes as we have no deploy support from others
                systemProperties = new HashMap<>(systemProperties);
                boolean shouldBuildImage = imageBuild || imageBuilder != null && !imageBuilder.isEmpty();
                systemProperties.put("quarkus." + getDeployer().name() + ".deploy", "true");
                systemProperties.put("quarkus.container-image.build", String.valueOf(shouldBuildImage));
                super.doExecute();
            } else if (targets.size() > 1 && target == null) {
                getLog().error(
                        "Too many installed extensions support quarkus:deploy.  You must choose one by setting quarkus.deploy.target.");
                getLog().error("Extensions: " + targets.stream().collect(Collectors.joining(" ")));
            } else if (target != null && !targets.contains(target)) {
                getLog().error("Unknown quarkus.deploy.target: " + target);
                getLog().error("Extensions: " + targets.stream().collect(Collectors.joining(" ")));
            } else {
                forceDependencies = false;
                if (target == null) {
                    target = targets.get(0);
                }
                AugmentAction deployAction = curatedApplication.createAugmentor();
                getLog().info("Deploy target: " + target);
                System.setProperty("quarkus.deploy.target", target);

                deployAction.performCustomBuild(DeployCommandHandler.class.getName(), new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean success) {
                    }
                }, DeployCommandActionResultBuildItem.class.getName());
            }
        } finally {

        }
    }
}
