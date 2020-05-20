package io.quarkus.funqy.runtime.bindings.knative.events;

import io.quarkus.funqy.runtime.FunqyServerRequest;
import io.quarkus.funqy.runtime.RequestContext;

public class FunqyRequestImpl implements FunqyServerRequest {
    protected RequestContext requestContext;
    protected Object input;

    public FunqyRequestImpl(RequestContext requestContext, Object input) {
        this.requestContext = requestContext;
        this.input = input;
    }

    @Override
    public RequestContext context() {
        return requestContext;
    }

    @Override
    public Object extractInput(Class inputClass) {
        return input;
    }
}
