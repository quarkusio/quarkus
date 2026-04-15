package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class DefaultValueHolder {

    private String mode = "auto";
    private String label;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
