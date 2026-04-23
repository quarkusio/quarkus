package io.quarkus.signals.deployment;

import java.util.EnumSet;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.signals.spi.Receiver.ExecutionModel;

final class ReceiverExecutorImplementationBuildItem extends SimpleBuildItem {

    private final ReceiverExecutorImplementation implementation;

    ReceiverExecutorImplementationBuildItem(ReceiverExecutorImplementation implementation) {
        this.implementation = implementation;
    }

    ReceiverExecutorImplementation getImplementation() {
        return implementation;
    }

    boolean isSupported(ExecutionModel model) {
        return implementation.getSupportedModels().contains(model);
    }

    enum ReceiverExecutorImplementation {
        VERTX(EnumSet.allOf(ExecutionModel.class)),
        DEFAULT_BLOCKING(EnumSet.of(ExecutionModel.BLOCKING));

        private Set<ExecutionModel> supportedModels;

        ReceiverExecutorImplementation(Set<ExecutionModel> supportedModels) {
            this.supportedModels = supportedModels;
        }

        Set<ExecutionModel> getSupportedModels() {
            return supportedModels;
        }
    }

}
