package org.jboss.resteasy.reactive.common.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

public class StatusTypeImpl implements StatusType {

    public static final String DEFAULT_REASON_PHRASE = "Unknown code"; // needed to avoid NPE in the TCK

    private final String reasonPhrase;
    private final int status;

    private StatusTypeImpl(int status, String reasonPhrase) {
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

    // return an immutable StatusType
    public static StatusType valueOf(StatusType statusType) {
        if (statusType instanceof Response.Status || statusType instanceof StatusTypeImpl) {
            return statusType;
        }
        return create(statusType.getStatusCode(), statusType.getReasonPhrase());
    }

    public static StatusType create(int status, String reasonPhrase) {
        StatusType statusType = Response.Status.fromStatusCode(status);
        return statusType != null ? statusType : new StatusTypeImpl(status, reasonPhrase);
    }
}
