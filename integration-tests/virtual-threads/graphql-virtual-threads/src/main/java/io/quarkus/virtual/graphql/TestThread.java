package io.quarkus.virtual.graphql;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Holds info about a thread
 */
public class TestThread {

    private long id;
    private String name;
    private int priority;
    private String state;
    private String group;

    public TestThread() {
        super();
    }

    public TestThread(long id, String name, int priority, String state, String group) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.state = state;
        this.group = group;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVertxContextClassName() {
        Context vc = Vertx.currentContext();
        return vc.getClass().getName();
    }
}
