package io.quarkus.smallrye.faulttolerance.test.reuse.config;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class HelloService {
    static final String OK = "Hello";

    @ApplyGuard("my-guard")
    public String hello(Exception exception) throws Exception {
        if (exception != null) {
            throw exception;
        }

        return OK;
    }
}
