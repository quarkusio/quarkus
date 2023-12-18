package io.quarkus.jfr.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.config.ConfigProvider;

@RequestScoped
public class RequestIdProducerImpl implements RequestIdProducer {

    private final static String REQUEST_ID_HEADER = ConfigProvider.getConfig()
            .getOptionalValue("quarkus.jfr.request-id-header", String.class).orElse("X-Request-ID");

    @Context
    HttpHeaders headers;

    public RequestId create() {

        String requestId = headers.getHeaderString(REQUEST_ID_HEADER);

        if (requestId != null) {
            return new RequestId(requestId);
        }

        return new RequestId(null);
    }
}
