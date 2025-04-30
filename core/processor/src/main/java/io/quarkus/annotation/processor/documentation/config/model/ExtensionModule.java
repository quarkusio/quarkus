package io.quarkus.annotation.processor.documentation.config.model;

public record ExtensionModule(String groupId, String artifactId, ExtensionModuleType type, Extension extension,
        boolean detected) {

    public static ExtensionModule createNotDetected() {
        return new ExtensionModule("not.detected", "not.detected", ExtensionModuleType.UNKNOWN, Extension.createNotDetected(),
                false);
    }

    public static ExtensionModule of(String groupId, String artifactId, ExtensionModuleType type, Extension extension) {
        return new ExtensionModule(groupId, artifactId, type, extension, true);
    }

    public enum ExtensionModuleType {
        RUNTIME,
        DEPLOYMENT,
        UNKNOWN;
    }
}
