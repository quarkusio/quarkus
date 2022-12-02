package io.quarkus.funqy.runtime;

public interface ValueInjector {
    Object extract(FunqyServerRequest request);
}
