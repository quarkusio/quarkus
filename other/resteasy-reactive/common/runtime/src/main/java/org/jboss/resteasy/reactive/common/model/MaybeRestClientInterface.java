package org.jboss.resteasy.reactive.common.model;

public class MaybeRestClientInterface {
    private final RestClientInterface restClientInterface;
    private final String failure;

    private MaybeRestClientInterface(RestClientInterface restClientInterface, String failure) {
        this.restClientInterface = restClientInterface;
        this.failure = failure;
    }

    public String getFailure() {
        return failure;
    }

    public boolean exists() {
        return restClientInterface != null;
    }

    public RestClientInterface getRestClientInterface() {
        return restClientInterface;
    }

    public static MaybeRestClientInterface failure(String failure) {
        return new MaybeRestClientInterface(null, failure);
    }

    public static MaybeRestClientInterface success(RestClientInterface restClientInterface) {
        return new MaybeRestClientInterface(restClientInterface, null);
    }
}
