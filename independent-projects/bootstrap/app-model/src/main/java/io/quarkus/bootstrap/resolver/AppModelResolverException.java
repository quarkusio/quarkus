package io.quarkus.bootstrap.resolver;

public class AppModelResolverException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AppModelResolverException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppModelResolverException(String message) {
        super(message);
    }
}
