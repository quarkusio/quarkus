package io.quarkus.rest.common.runtime.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

public class QuarkusRestStatusType implements StatusType {

    public static final String DEFAULT_REASON_PHRASE = "Unknown code"; // needed to avoid NPE in the TCK

    private final String reasonPhrase;
    private final int status;

    public QuarkusRestStatusType(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase != null ? reasonPhrase : DEFAULT_REASON_PHRASE;
    }

    @Override
    public int getStatusCode() {
        return status;
    }

    @Override
    public Family getFamily() {
        return Response.Status.Family.familyOf(status);
    }

    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public static QuarkusRestStatusType valueOf(StatusType statusType) {
        if (statusType instanceof QuarkusRestStatusType)
            return (QuarkusRestStatusType) statusType;
        return new QuarkusRestStatusType(statusType.getStatusCode(), statusType.getReasonPhrase());
    }
}
