package io.quarkus.bootstrap;

import io.quarkus.bootstrap.resolver.AppModelResolverException;

public class BootstrapGradleException extends AppModelResolverException {

    public BootstrapGradleException(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapGradleException(String message) {
        super(message);
    }
}