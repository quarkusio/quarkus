package io.quarkus.restclient.config.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.restclient.config.AbstractRestClientConfigBuilder;
import io.quarkus.restclient.config.RegisteredRestClient;
import io.quarkus.runtime.configuration.ConfigBuilder;

public final class RestClientConfigUtils {

    private RestClientConfigUtils() {
        throw new UnsupportedOperationException();
    }

    public static void generateRestClientConfigBuilder(
            List<RegisteredRestClient> restClients,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        String className = "io.quarkus.runtime.generated.RestClientConfigBuilder";
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(className)
                .superClass(AbstractRestClientConfigBuilder.class)
                .interfaces(ConfigBuilder.class)
                .setFinal(true)
                .build()) {

            MethodCreator method = classCreator.getMethodCreator(
                    MethodDescriptor.ofMethod(AbstractRestClientConfigBuilder.class, "getRestClients", List.class));

            ResultHandle list = method.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
            for (RegisteredRestClient restClient : restClients) {
                ResultHandle restClientElement = method.newInstance(
                        MethodDescriptor.ofConstructor(RegisteredRestClient.class, String.class, String.class, String.class),
                        method.load(restClient.getFullName()),
                        method.load(restClient.getSimpleName()),
                        restClient.getConfigKey() != null ? method.load(restClient.getConfigKey()) : method.loadNull());

                method.invokeVirtualMethod(MethodDescriptor.ofMethod(ArrayList.class, "add", boolean.class, Object.class), list,
                        restClientElement);
            }

            method.returnValue(list);
        }

        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(className));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(className));
    }
}
