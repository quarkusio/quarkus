package io.quarkus.vertx.http;

import java.util.Optional;
import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.core.deployment.service.impl.ServiceMetadataBuildItem;
import io.quarkus.core.deployment.service.impl.ServiceRegistrarImpl;
import io.quarkus.core.deployment.service.impl.ServiceValueRetentionBuildItem;
import io.quarkus.core.deployment.service.impl.StaticServiceMetadataBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.runtime.Address;

/**
 * A startup service that depends on the listener addresses through the service graph, the way an extension would
 * with {@code request(Address.class, "vertx-http")}. What it received is kept in static fields, so a test can assert
 * it. The class must be added to the application archive, since the service body runs at runtime.
 */
public final class AddressServiceConsumer {
    public static volatile Optional<Address> main;
    public static volatile Optional<Address> management;

    private AddressServiceConsumer() {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers the service from a build chain customizer. A raw {@link BuildStep} does not receive a
     * {@link ServiceRegistrar}, so this does what the extension loader does for a {@code @BuildStep} method: it
     * creates the registrar over the step's producers, and declares the build items the registrar produces.
     */
    static Consumer<BuildChainBuilder> buildStep() {
        String stepId = AddressServiceConsumer.class.getName() + "#requestAddresses";
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                requestAddresses(context, stepId);
            }

            @Override
            public String getId() {
                return stepId;
            }
        })
                .produces(MainBytecodeRecorderBuildItem.class)
                .produces(StaticBytecodeRecorderBuildItem.class)
                .produces(ServiceMetadataBuildItem.class)
                .produces(StaticServiceMetadataBuildItem.class)
                .produces(ServiceValueRetentionBuildItem.class)
                .build();
    }

    // static, so the service lambda cannot capture the build step instance
    private static void requestAddresses(BuildContext context, String stepId) {
        ServiceRegistrar registrar = new ServiceRegistrarImpl(context::produce, context::produce, context::produce,
                context::produce, context::produce, stepId);
        registrar.forService("io.quarkus.vertx.http.test.address-consumer")
                .request(Address.class, HttpServer.VERTX_HTTP)
                .request(Address.class, HttpServer.VERTX_MANAGEMENT)
                .onStart((ctx, main, management) -> {
                    AddressServiceConsumer.main = main;
                    AddressServiceConsumer.management = management;
                });
    }
}
