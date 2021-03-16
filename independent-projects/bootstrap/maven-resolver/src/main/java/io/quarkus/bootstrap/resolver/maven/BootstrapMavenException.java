package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.AppModelResolverException;

@SuppressWarnings("serial")
public class BootstrapMavenException extends AppModelResolverException {

    public BootstrapMavenException(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapMavenException(String message) {
        super(message);
    }
}
