package io.quarkus.azure.functions.resteasy.deployment;

import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.LegacyJarRequiredBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AzureFunctionsHttpProcessor {
    private static final Logger log = Logger.getLogger(AzureFunctionsHttpProcessor.class);

    @BuildStep
    public LegacyJarRequiredBuildItem forceLegacy(List<FeatureBuildItem> features, PackageConfig config) throws BuildException {
        for (FeatureBuildItem item : features) {
            if (Feature.AZURE_FUNCTIONS.getName().equals(item.getName())) {
                throw new BuildException(
                        "quarkus-azure-functions-http extension is incompatible with quarkus-azure-functions extension.  Remove quarkus-azure-functions from your build.",
                        Collections.EMPTY_LIST);
            }
        }
        // Azure Functions need a legacy jar and no runner
        config.addRunnerSuffix = false;
        return new LegacyJarRequiredBuildItem();
    }

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }
}
