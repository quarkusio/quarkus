package io.quarkus.oidc.common.runtime;

@SuppressWarnings("serial")
public class OidcEndpointAccessException extends RuntimeException {

    private final int errorStatus;

    public OidcEndpointAccessException(int errorStatus) {
        this.errorStatus = errorStatus;
    }

    public int getErrorStatus() {
        return errorStatus;
    }

}
