package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class HighPriorityExceptionMapper implements ResponseExceptionMapper<DummyException3> {

    @Override
    public DummyException3 toThrowable(Response response) {
        return new DummyException3();
    }

    @Override
    public int getPriority() {
        return Priorities.USER - 1;
    }
}
