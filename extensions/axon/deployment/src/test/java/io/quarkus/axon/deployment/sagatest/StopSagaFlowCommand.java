package io.quarkus.axon.deployment.sagatest;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class StopSagaFlowCommand {
    @TargetAggregateIdentifier
    private String id;

    public StopSagaFlowCommand() {
    }

    public StopSagaFlowCommand(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
