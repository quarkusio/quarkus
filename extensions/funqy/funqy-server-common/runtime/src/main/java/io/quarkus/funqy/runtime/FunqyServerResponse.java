package io.quarkus.funqy.runtime;

import io.smallrye.mutiny.Uni;

public interface FunqyServerResponse {

    Uni<?> getOutput();

    void setOutput(Uni<?> out);

}
