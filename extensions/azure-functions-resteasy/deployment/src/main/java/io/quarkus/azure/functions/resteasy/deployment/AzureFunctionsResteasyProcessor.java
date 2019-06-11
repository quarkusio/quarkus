package io.quarkus.azure.functions.resteasy.deployment;

import org.jboss.logging.Logger;

import io.quarkus.azure.functions.resteasy.runtime.AzureFunctionsResteasyTemplate;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;

public class AzureFunctionsResteasyProcessor {
    private static final Logger log = Logger.getLogger(AzureFunctionsResteasyProcessor.class);

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem("azure-resteasy");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void build(ResteasyDeploymentBuildItem deployment, AzureFunctionsResteasyTemplate template) {
        template.start(deployment.getRootPath(), deployment.getDeployment());
    }
}
