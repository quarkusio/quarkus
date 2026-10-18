package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class Capability {

    private String name;

    private List<String> onlyIf = new ArrayList<>(0);

    private List<String> onlyIfNot = new ArrayList<>(0);

    public Capability(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Capability onlyIf(List<String> conditions) {
        onlyIf.addAll(conditions);
        return this;
    }

    public List<String> getOnlyIf() {
        return onlyIf;
    }

    public Capability onlyIfNot(List<String> conditions) {
        onlyIfNot.addAll(conditions);
        return this;
    }

    public List<String> getOnlyIfNot() {
        return onlyIfNot;
    }
}
