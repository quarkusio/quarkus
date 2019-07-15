package io.quarkus.axon.deployment.sagatest;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class NextSagaFlowStepCommand {
    @TargetAggregateIdentifier
    private String id;

    public NextSagaFlowStepCommand() {
    }

    public NextSagaFlowStepCommand(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
