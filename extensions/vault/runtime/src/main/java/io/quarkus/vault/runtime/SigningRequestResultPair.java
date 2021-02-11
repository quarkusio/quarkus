package io.quarkus.vault.runtime;

import io.quarkus.vault.runtime.transit.SigningResult;
import io.quarkus.vault.transit.SigningRequest;

public class SigningRequestResultPair {

    public static final int NO_KEY_VERSION = -1;

    private SigningRequest request;
    private SigningResult result;

    public SigningRequestResultPair(SigningRequest request) {
        this.request = request;
    }

    public int getKeyVersion() {
        return request.getKeyVersion() == null ? NO_KEY_VERSION : request.getKeyVersion();
    }

    public void setResult(SigningResult result) {
        this.result = result;
    }

    public SigningResult getResult() {
        return result;
    }

    public SigningRequest getRequest() {
        return request;
    }
}
