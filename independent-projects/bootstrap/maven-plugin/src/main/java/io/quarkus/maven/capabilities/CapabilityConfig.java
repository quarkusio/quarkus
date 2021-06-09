package io.quarkus.maven.capabilities;

import java.util.ArrayList;
import java.util.List;

public class CapabilityConfig {

    private String name;
    private final List<String> onlyIf = new ArrayList<>(0);
    private final List<String> onlyIfNot = new ArrayList<>(0);

    public void set(String name) {
        this.name = name;
    }

    public void addPositive(String supplier) {
        this.onlyIf.add(supplier);
    }

    public void addNegative(String supplier) {
        this.onlyIfNot.add(supplier);
    }

    public String getName() {
        return name;
    }

    public List<String> getOnlyIf() {
        return onlyIf;
    }

    public List<String> getOnlyIfNot() {
        return onlyIfNot;
    }
}
