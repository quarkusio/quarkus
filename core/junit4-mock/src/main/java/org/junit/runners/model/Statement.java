package org.junit.runners.model;

public abstract class Statement {
    public Statement() {
    }

    public abstract void evaluate() throws Throwable;
}
