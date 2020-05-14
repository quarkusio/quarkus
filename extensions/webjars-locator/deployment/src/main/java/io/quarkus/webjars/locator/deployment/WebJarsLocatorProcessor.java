package io.quarkus.webjars.locator.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.webjars.locator.runtime.WebJarsLocatorRecorder;

public class WebJarsLocatorProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findWebjarsAndCreateHandler(
            HttpBuildTimeConfig httpConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            WebJarsLocatorRecorder recorder) throws Exception {
        // The context path + the resources path
        String rootPath = httpConfig.rootPath;
        String webjarRootPath = (rootPath.endsWith("/")) ? rootPath + "webjars/" : rootPath + "/webjars/";
        feature.produce(new FeatureBuildItem(FeatureBuildItem.WEBJARS_LOCATOR));
        routes.produce(
                new RouteBuildItem(webjarRootPath + "*",
                        recorder.getHandler(webjarRootPath),
                        false));
    }

}
