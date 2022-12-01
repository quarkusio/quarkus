package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.smallrye.mutiny.Uni;

public class FunqyResponseImpl implements FunqyServerResponse {
    protected Uni<?> output;

    @Override
    public Uni<?> getOutput() {
        return output;
    }

    @Override
    public void setOutput(Uni<?> output) {
        this.output = output;
    }
}
