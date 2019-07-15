package io.quarkus.axon.deployment.sagatest;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class StartSagaFlowCommand {
    @TargetAggregateIdentifier
    private String id;

    public StartSagaFlowCommand() {
    }

    public StartSagaFlowCommand(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
