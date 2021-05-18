package io.quarkus.webjar.locator.deployment;

import java.util.Map;

import org.jboss.logging.Logger;
import org.webjars.WebJarAssetLocator;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.webjar.locator.runtime.WebJarLocatorRecorder;

public class WebJarLocatorStandaloneBuildStep {

    private static final Logger log = Logger.getLogger(WebJarLocatorStandaloneBuildStep.class.getName());

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findWebjarsAndCreateHandler(
            HttpBuildTimeConfig httpConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            WebJarLocatorRecorder recorder) throws Exception {

        WebJarAssetLocator webJarLocator = new WebJarAssetLocator();
        Map<String, String> webjarNameToVersionMap = webJarLocator.getWebJars();
        if (!webjarNameToVersionMap.isEmpty()) {
            // The context path + the resources path
            String rootPath = httpConfig.rootPath;
            String webjarRootPath = (rootPath.endsWith("/")) ? rootPath + "webjars/" : rootPath + "/webjars/";
            feature.produce(new FeatureBuildItem(Feature.WEBJARS_LOCATOR));
            routes.produce(
                    RouteBuildItem.builder().route(webjarRootPath + "*")
                            .handler(recorder.getHandler(webjarRootPath, webjarNameToVersionMap)).build());
        } else {
            log.warn("No WebJars were found in the project. Requests to the /webjars/ path will always return 404 (Not Found)");
        }

    }

}
