package io.quarkus.oidc.proxy.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.util.UriNormalizationUtil;
import io.quarkus.oidc.proxy.runtime.OidcProxyRecorder;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

@BuildSteps(onlyIf = OidcProxyBuildStep.IsEnabled.class)
public class OidcProxyBuildStep {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(
            OidcProxyRecorder recorder,
            VertxWebRouterBuildItem vertxWebRouterBuildItem,
            HttpBuildTimeConfig httpBuildTimeConfig,
            BeanContainerBuildItem beanContainerBuildItem) {
        recorder.setupRoutes(beanContainerBuildItem.getValue(), vertxWebRouterBuildItem.getHttpRouter(),
                UriNormalizationUtil.toURI(httpBuildTimeConfig.rootPath, false).toString());
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcProxyBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
