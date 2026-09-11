package io.quarkus.gradle.application.internal.execution.worker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.cmd.DeployCommandActionResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.gradle.application.internal.deployment.DeploymentResult;
import io.quarkus.gradle.application.internal.deployment.DeploymentResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

public abstract class DeployWorker extends QuarkusWorker<DeployWorkerParams> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployWorker.class);
    private static final String DEPLOY_COMMAND_RESULT_HANDLER = "io.quarkus.deployment.cmd.DeployCommandResultHandler";
    private static final String SUCCESS = "success";
    private static final String RESULT_NAME = "result.name";
    private static final String RESULT_LABEL_PREFIX = "result.labels.";

    @Override
    public void execute() {
        DeployWorkerParams params = getParameters();
        LOGGER.info("Deploying Quarkus application {} to {}", params.getBuildName().get(),
                params.getDeploymentTarget().get());

        try (CuratedApplication appCreationContext = createAppCreationContext()) {
            AtomicReference<Map<String, String>> resultReference = new AtomicReference<>(Map.of());
            AugmentAction deployAction = appCreationContext.createAugmentor();
            deployAction.performCustomBuild(DEPLOY_COMMAND_RESULT_HANDLER,
                    new Consumer<Map<String, String>>() {
                        @Override
                        public void accept(Map<String, String> result) {
                            resultReference.set(result);
                        }
                    },
                    DeployCommandActionResultBuildItem.class.getName(),
                    DeploymentResultBuildItem.class.getName());
            Map<String, String> result = resultReference.get();
            if (!Boolean.parseBoolean(result.getOrDefault(SUCCESS, "false"))) {
                throw new GradleException("Quarkus deployment command for '" + params.getBuildName().get()
                        + "/" + params.getDeploymentName().get() + "' did not report success");
            }
            new DeploymentResultCodec().write(
                    params.getDeploymentResultFile().get().getAsFile().toPath(),
                    deploymentResult(params, result));
        } catch (BootstrapException e) {
            throw new GradleException("Failed to deploy Quarkus application '" + params.getBuildName().get()
                    + "/" + params.getDeploymentName().get() + "' due to " + e, e);
        }
    }

    private static DeploymentResult deploymentResult(DeployWorkerParams params,
            Map<String, String> result) {
        String target = params.getDeploymentTarget().get();
        return new DeploymentResult(
                params.getBuildName().get(),
                params.getDeploymentName().get(),
                target(target),
                QuarkusApplicationDeploymentImageSource.valueOf(params.getImageSource().get()),
                params.getImageReference().get(),
                Optional.of(target),
                Optional.of(target),
                Optional.ofNullable(result.get(RESULT_NAME)),
                labels(result),
                true);
    }

    private static QuarkusApplicationDeploymentTarget target(String target) {
        for (QuarkusApplicationDeploymentTarget candidate : QuarkusApplicationDeploymentTarget.values()) {
            if (candidate.quarkusDeployTarget().equals(target)) {
                return candidate;
            }
        }
        throw new GradleException("Unknown deployment target: " + target);
    }

    private static Map<String, String> labels(Map<String, String> result) {
        Map<String, String> labels = new LinkedHashMap<>();
        result.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(RESULT_LABEL_PREFIX))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> labels.put(entry.getKey().substring(RESULT_LABEL_PREFIX.length()),
                        entry.getValue()));
        return labels;
    }
}
