package io.quarkus.axon.deployment.aggregatetest;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class CreateTestItemCommand {
    @TargetAggregateIdentifier
    private String id;
    private String customerId;
    private double balance;

    public CreateTestItemCommand() {

    }

    public CreateTestItemCommand(String id, String customerId, double balance) {
        this.id = id;
        this.customerId = customerId;
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "CreateTestItemCommand{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", balance=" + balance +
                '}';
    }
}
