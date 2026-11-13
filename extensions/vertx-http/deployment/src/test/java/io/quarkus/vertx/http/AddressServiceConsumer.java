package io.quarkus.vertx.http;

import java.util.function.Consumer;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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
 * A service that depends on the HTTP server addresses through the service graph, the way an extension would with
 * {@code request(Address.class, "vertx-http")}. Its value, what it received, is exposed as a CDI bean, so a test
 * injects {@link ReceivedAddresses} from its own application and no state is shared between tests. This class and
 * {@link ReceivedAddresses} must be added to the application archive, since the service body runs at runtime.
 */
public final class AddressServiceConsumer {
    private AddressServiceConsumer() {
        throw new UnsupportedOperationException();
    }

    static Consumer<BuildChainBuilder> buildStep() {
        String stepId = AddressServiceConsumer.class.getName() + "#requestAddresses";
        return builder -> {
            builder.addBuildStep(new BuildStep() {
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

            builder.addBuildStep(new BuildStep() {
                @Override
                public void execute(BuildContext context) {
                    context.produce(SyntheticBeanBuildItem.configure(ReceivedAddresses.class)
                            .setRuntimeInit()
                            .unremovable()
                            .serviceValue(ReceivedAddresses.class)
                            .scope(Singleton.class)
                            .done());
                }
            })
                    .produces(SyntheticBeanBuildItem.class)
                    .build();
        };
    }

    private static void requestAddresses(BuildContext context, String stepId) {
        ServiceRegistrar registrar = new ServiceRegistrarImpl(context::produce, context::produce, context::produce,
                context::produce, context::produce, stepId);
        registrar.forService(ReceivedAddresses.class)
                .request(Address.class, HttpServer.VERTX_HTTP)
                .request(Address.class, HttpServer.VERTX_MANAGEMENT)
                .onStart((ctx, main, management) -> new ReceivedAddresses(main, management));
    }
}
