package io.quarkus.axon.deployment.sagatest;

public class TestSagaEndEvent {

    private String id;

    public TestSagaEndEvent() {
    }

    public TestSagaEndEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
