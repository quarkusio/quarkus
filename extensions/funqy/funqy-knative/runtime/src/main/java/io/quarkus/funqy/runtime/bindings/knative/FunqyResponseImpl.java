package io.quarkus.funqy.runtime.bindings.knative;

import io.quarkus.funqy.runtime.FunqyServerResponse;

public class FunqyResponseImpl implements FunqyServerResponse {
    protected Object output;

    @Override
    public Object getOutput() {
        return output;
    }

    @Override
    public void setOutput(Object output) {
        this.output = output;
    }
}
