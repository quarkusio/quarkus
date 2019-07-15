package io.quarkus.axon.deployment.aggregatetest;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class TestItemCreatedEvent {
    @TargetAggregateIdentifier
    private String id;
    private String customerId;
    private double balance;

    public TestItemCreatedEvent() {
    }

    public TestItemCreatedEvent(String id, String customerId, double balance) {
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
        return "TestItemCreatedEvent{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", balance=" + balance +
                '}';
    }
}
