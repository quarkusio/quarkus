package io.quarkus.claesh;

import io.quarkus.arc.ArcContainer;

public class QuarkusContext {

    private final ArcContainer arcContainer;

    public QuarkusContext(ArcContainer arcContainer) {
        this.arcContainer = arcContainer;
    }

    public String name() {
        return "Quarkus";
    }

    public ArcContainer getArcContainer() {
        return arcContainer;
    }
}
