package io.quarkus.funqy.lambda;

import java.util.concurrent.CompletionStage;

import io.quarkus.funqy.runtime.FunqyServerResponse;

public class FunqyResponseImpl implements FunqyServerResponse {
    protected CompletionStage<?> output;

    @Override
    public CompletionStage<?> getOutput() {
        return output;
    }

    @Override
    public void setOutput(CompletionStage<?> output) {
        this.output = output;
    }
}
