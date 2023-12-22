package io.quarkus.jfr.runtime;

import org.eclipse.microprofile.config.ConfigProvider;

public interface RequestIdProducer {

    String REQUEST_ID_HEADER = ConfigProvider.getConfig()
            .getOptionalValue("quarkus.jfr.request-id-header", String.class).orElse("X-Request-ID");

    RequestId create();
}
