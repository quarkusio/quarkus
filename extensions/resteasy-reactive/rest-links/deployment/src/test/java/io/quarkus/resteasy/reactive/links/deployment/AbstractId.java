package io.quarkus.resteasy.reactive.links.deployment;

public abstract class AbstractId {

    private int id;

    public AbstractId() {
    }

    protected AbstractId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}