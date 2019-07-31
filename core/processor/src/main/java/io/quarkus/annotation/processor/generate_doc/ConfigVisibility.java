package io.quarkus.annotation.processor.generate_doc;

public enum ConfigVisibility {
    RUN_TIME("overridable at runtime"),
    BUILD_TIME("visible at build time only"),
    BUILD_AND_RUN_TIME_FIXED("visible at build and runtime time, read only at runtime");

    private String description;

    ConfigVisibility(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
