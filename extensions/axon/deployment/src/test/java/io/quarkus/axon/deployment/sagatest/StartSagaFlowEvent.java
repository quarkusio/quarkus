package io.quarkus.axon.deployment.sagatest;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class StartSagaFlowEvent {
    @TargetAggregateIdentifier
    private String id;

    public StartSagaFlowEvent() {
    }

    public StartSagaFlowEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
