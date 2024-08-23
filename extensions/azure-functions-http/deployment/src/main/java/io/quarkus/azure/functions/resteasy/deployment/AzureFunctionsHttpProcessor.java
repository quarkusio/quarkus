package io.quarkus.azure.functions.resteasy.deployment;

import java.lang.reflect.Method;

import org.jboss.logging.Logger;

import io.quarkus.azure.functions.deployment.AzureFunctionBuildItem;
import io.quarkus.azure.functions.resteasy.runtime.Function;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AzureFunctionsHttpProcessor {
    private static final Logger log = Logger.getLogger(AzureFunctionsHttpProcessor.class);

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }

    @BuildStep
    public void registerFunction(BuildProducer<AzureFunctionBuildItem> producer) {
        Method functionMethod = null;
        for (Method method : Function.class.getMethods()) {
            if (method.getName().equals("run")) {
                functionMethod = method;
                break;
            }
        }
        producer.produce(new AzureFunctionBuildItem(Function.QUARKUS_HTTP, Function.class, functionMethod));
    }
}
