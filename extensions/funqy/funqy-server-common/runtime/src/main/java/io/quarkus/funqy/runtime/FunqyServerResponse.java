package io.quarkus.funqy.runtime;

public interface FunqyServerResponse {
    Object getOutput();

    void setOutput(Object out);

}
