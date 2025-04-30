package io.quarkus.arc.deployment.devui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

// this class is present in the `arc/deployment` module, because it has to be present always
// and cannot be in the `core/deployment` module, as it depends on `quarkus-vertx-http-dev-ui-spi`
public class JsonRpcMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem jsonRpcMethods(List<JsonRPCProvidersBuildItem> rpcProviders) {
        Set<String> classes = new HashSet<>();
        for (JsonRPCProvidersBuildItem rpcProvider : rpcProviders) {
            classes.add(rpcProvider.getJsonRPCMethodProviderClass().getName());
        }

        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return classes.contains(method.declaringClass().name().toString());
            }
        });
    }
}
