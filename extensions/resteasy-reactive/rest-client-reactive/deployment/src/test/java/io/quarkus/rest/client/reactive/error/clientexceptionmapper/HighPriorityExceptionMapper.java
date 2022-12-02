package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;

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
