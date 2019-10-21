package io.quarkus.vault.runtime.client;

import io.quarkus.vault.VaultException;

public class VaultClientException extends VaultException {

    private int status;
    private String body;

    public VaultClientException(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return super.toString() + " code=" + status + " body=" + body;
    }
}
