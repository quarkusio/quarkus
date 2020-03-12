package io.quarkus.platform.descriptor.resolver.json;

public class PlatformDescriptorLoadingException extends Exception {

    private static final long serialVersionUID = 1L;

    public PlatformDescriptorLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlatformDescriptorLoadingException(String message) {
        super(message);
    }
}
