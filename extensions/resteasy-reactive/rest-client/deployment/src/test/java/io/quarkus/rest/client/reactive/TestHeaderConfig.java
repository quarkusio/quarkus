package io.quarkus.rest.client.reactive;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.arc.Unremovable;

@RequestScoped
@Unremovable
public class TestHeaderConfig {

    public static final String HEADER_PARAM_NAME = "filterheader";

    public String getHeaderPropertyName() {
        return HEADER_PARAM_NAME;
    }
}
