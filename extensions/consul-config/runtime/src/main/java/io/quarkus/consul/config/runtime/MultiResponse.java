package io.quarkus.consul.config.runtime;

import java.util.ArrayList;
import java.util.List;

public class MultiResponse {

    private final List<Response> responses = new ArrayList<>();

    public MultiResponse addResponse(Response response) {
        responses.add(response);
        return this;
    }

    public List<Response> getResponses() {
        return responses;
    }
}
