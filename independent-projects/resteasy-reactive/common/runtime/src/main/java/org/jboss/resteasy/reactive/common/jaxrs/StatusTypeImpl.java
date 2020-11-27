package org.jboss.resteasy.reactive.common.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

public class StatusTypeImpl implements StatusType {

    public static final String DEFAULT_REASON_PHRASE = "Unknown code"; // needed to avoid NPE in the TCK

    private final String reasonPhrase;
    private final int status;

    public StatusTypeImpl(int status, String reasonPhrase) {
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

    public static StatusTypeImpl valueOf(StatusType statusType) {
        if (statusType instanceof StatusTypeImpl)
            return (StatusTypeImpl) statusType;
        return new StatusTypeImpl(statusType.getStatusCode(), statusType.getReasonPhrase());
    }
}
