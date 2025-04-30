package io.quarkus.restclient.config.deployment;

import static io.quarkus.restclient.config.Constants.GLOBAL_REST_SCOPE_FORMAT;
import static io.quarkus.restclient.config.Constants.MP_REST_SCOPE_FORMAT;
import static io.quarkus.restclient.config.Constants.QUARKUS_REST_SCOPE_FORMAT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.jboss.jandex.ClassInfo;

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
    }

    public static Optional<String> findConfiguredScope(Config config, ClassInfo restClientInterface,
            Optional<String> configKeyOptional) {
        Optional<String> scopeConfig;

        // quarkus style config; fully qualified class name
        scopeConfig = config.getOptionalValue(
                String.format(QUARKUS_REST_SCOPE_FORMAT, '"' + restClientInterface.name().toString() + '"'),
                String.class);
        if (scopeConfig.isEmpty()) { // microprofile style config; fully qualified class name
            scopeConfig = config.getOptionalValue(
                    String.format(MP_REST_SCOPE_FORMAT, restClientInterface.name().toString()),
                    String.class);
        }
        if (scopeConfig.isEmpty() && configKeyOptional.isPresent()) { // quarkus style config; configKey
            scopeConfig = config.getOptionalValue(String.format(QUARKUS_REST_SCOPE_FORMAT, configKeyOptional.get()),
                    String.class);
        }
        if (scopeConfig.isEmpty() && configKeyOptional.isPresent()) { // quarkus style config; quoted configKey
            scopeConfig = config.getOptionalValue(String.format(QUARKUS_REST_SCOPE_FORMAT, '"' + configKeyOptional.get() + '"'),
                    String.class);
        }
        if (scopeConfig.isEmpty() && configKeyOptional.isPresent()) { // microprofile style config; configKey
            scopeConfig = config.getOptionalValue(String.format(MP_REST_SCOPE_FORMAT, configKeyOptional.get()), String.class);
        }
        if (scopeConfig.isEmpty()) { // quarkus style config; short class name
            scopeConfig = config.getOptionalValue(
                    String.format(QUARKUS_REST_SCOPE_FORMAT, restClientInterface.simpleName()),
                    String.class);
        }
        return scopeConfig;
    }

    public static Optional<String> getDefaultScope(Config config) {
        return config.getOptionalValue(GLOBAL_REST_SCOPE_FORMAT, String.class);
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
