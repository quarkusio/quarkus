package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

final class PackageProtectedBean {

    private final int value;
    private String label;

    public PackageProtectedBean(int value) {
        this.value = value;
        this.label = "item-" + value;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
