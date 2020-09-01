package io.quarkus.rest.runtime.jaxrs;

import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

public class QuarkusRestStatusType implements StatusType {

    private String reasonPhrase;
    private int status;

    public QuarkusRestStatusType(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public int getStatusCode() {
        return status;
    }

    @Override
    public Family getFamily() {
        return toEnum().getFamily();
    }

    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }

}
