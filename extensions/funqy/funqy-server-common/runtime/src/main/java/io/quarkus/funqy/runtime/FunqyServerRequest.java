package io.quarkus.funqy.runtime;

public interface FunqyServerRequest {
    RequestContext context();

    Object extractInput(Class inputClass);
}
