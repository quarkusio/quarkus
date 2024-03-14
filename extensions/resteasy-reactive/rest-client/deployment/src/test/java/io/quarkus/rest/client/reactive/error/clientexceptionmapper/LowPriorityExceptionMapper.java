package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class LowPriorityExceptionMapper implements ResponseExceptionMapper<DummyException2> {

    @Override
    public DummyException2 toThrowable(Response response) {
        return new DummyException2();
    }

    @Override
    public int getPriority() {
        return Priorities.USER + 1;
    }
}
