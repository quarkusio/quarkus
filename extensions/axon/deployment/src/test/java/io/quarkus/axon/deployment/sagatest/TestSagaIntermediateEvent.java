package io.quarkus.axon.deployment.sagatest;

public class TestSagaIntermediateEvent {

    private String id;

    public TestSagaIntermediateEvent() {
    }

    public TestSagaIntermediateEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
