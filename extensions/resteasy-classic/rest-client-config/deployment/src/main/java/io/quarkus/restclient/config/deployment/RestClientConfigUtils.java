package io.quarkus.restclient.config.deployment;

import java.util.List;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProvider,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        String className = "io.quarkus.runtime.generated.RestClientConfigBuilder";
        Gizmo gizmo = Gizmo
                .create(new GeneratedClassGizmo2Adaptor(generatedClass, generatedResource, generatedServiceProvider, true));
        gizmo.class_(className, cc -> {
            cc.final_();
            cc.extends_(AbstractRestClientConfigBuilder.class);
            cc.implements_(ConfigBuilder.class);
            cc.defaultConstructor();

            cc.method(MethodDesc.of(AbstractRestClientConfigBuilder.class, "getRestClients", List.class), mc -> {
                mc.body(b0 -> {
                    b0.return_(b0.listOf(restClients, rc -> b0.new_(
                            ConstructorDesc.of(RegisteredRestClient.class, String.class, String.class, String.class),
                            Const.of(rc.getFullName()),
                            Const.of(rc.getSimpleName()),
                            rc.getConfigKey() != null ? Const.of(rc.getConfigKey()) : Const.ofNull(String.class))));
                });
            });
        });

        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(className));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(className));
    }
}
