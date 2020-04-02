package io.quarkus.funqy.runtime;

import java.util.concurrent.CompletionStage;

public interface FunqyServerResponse {

    CompletionStage<?> getOutput();

    void setOutput(CompletionStage<?> out);

}
