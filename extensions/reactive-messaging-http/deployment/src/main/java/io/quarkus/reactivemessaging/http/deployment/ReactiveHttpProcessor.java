package io.quarkus.reactivemessaging.http.deployment;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.reactivemessaging.http.runtime.QuarkusHttpConnector;
import io.quarkus.reactivemessaging.http.runtime.QuarkusWebsocketConnector;
import io.quarkus.reactivemessaging.http.runtime.ReactiveHttpHandlerBean;
import io.quarkus.reactivemessaging.http.runtime.ReactiveHttpRecorder;
import io.quarkus.reactivemessaging.http.runtime.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.runtime.config.WebsocketStreamConfig;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 28/08/2019
 */
public class ReactiveHttpProcessor {

    @Inject
    BuildProducer<RouteBuildItem> routeProducer;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.REACTIVE_MESSAGING_HTTP);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerHttpConnector(BuildProducer<AdditionalBeanBuildItem> beanProducer,
            ReactiveHttpRecorder recorder) {
        beanProducer.produce(new AdditionalBeanBuildItem(QuarkusHttpConnector.class));
        beanProducer.produce(new AdditionalBeanBuildItem(QuarkusWebsocketConnector.class));
        beanProducer.produce(new AdditionalBeanBuildItem(ReactiveHttpConfig.class));
        beanProducer.produce(new AdditionalBeanBuildItem(ReactiveHttpHandlerBean.class));

        List<HttpStreamConfig> httpConfigs = ReactiveHttpConfig.readHttpConfigs();
        List<WebsocketStreamConfig> wsConfigs = ReactiveHttpConfig.readWebsocketConfigs();

        if (!httpConfigs.isEmpty()) {
            Handler<RoutingContext> handler = recorder.createHttpHandler();
            Handler<RoutingContext> bodyHandler = recorder.createBodyHandler();

            httpConfigs.stream()
                    .map(HttpStreamConfig::path)
                    .distinct()
                    .forEach(path -> registerRoute(path, bodyHandler, handler));
        }
        if (!wsConfigs.isEmpty()) {
            Handler<RoutingContext> handler = recorder.createWebsocketHandler();

            wsConfigs.stream()
                    .map(WebsocketStreamConfig::path)
                    .distinct()
                    .forEach(path -> registerRoute(path, handler));
        }
    }

    private void registerRoute(String path,
            Handler<RoutingContext>... handlers) {

        for (Handler<RoutingContext> handler : handlers) {
            routeProducer.produce(new RouteBuildItem(path, handler));
        }
    }
}
