package io.quarkus.jfr.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

@RequestScoped
public class RequestIdProducerImpl implements RequestIdProducer {

    @Context
    HttpHeaders headers;

    public RequestId create() {
        String requestId = headers.getHeaderString(REQUEST_ID_HEADER);
        return new RequestId(requestId);
    }
}
