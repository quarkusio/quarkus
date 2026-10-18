package org.jboss.resteasy.reactive.common.jaxrs;

import java.util.Objects;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

public class StatusTypeImpl implements StatusType {

    public static final String DEFAULT_REASON_PHRASE = "Unknown code"; // needed to avoid NPE in the TCK

    private final String reasonPhrase;
    private final int status;

    public StatusTypeImpl(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase != null ? reasonPhrase : getDefaultReasonPhrase(status);
    }

    private static String getDefaultReasonPhrase(int providedStatus) {
        for (Response.Status defaultStatus : Response.Status.values()) {
            if (providedStatus == defaultStatus.getStatusCode()) {
                return defaultStatus.getReasonPhrase() != null ? defaultStatus.getReasonPhrase() : DEFAULT_REASON_PHRASE;
            }
        }
        return DEFAULT_REASON_PHRASE;
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StatusType)) {
            return false;
        }
        return this.status == ((StatusType) other).getStatusCode()
                && this.reasonPhrase.equals(((StatusType) other).getReasonPhrase());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(status) + 37 * reasonPhrase.hashCode();
    }

    public static StatusTypeImpl valueOf(StatusType statusType) {
        if (statusType instanceof StatusTypeImpl)
            return (StatusTypeImpl) statusType;
        return new StatusTypeImpl(statusType.getStatusCode(), statusType.getReasonPhrase());
    }
}
