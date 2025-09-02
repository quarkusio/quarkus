package io.quarkus.it.faulttolerance;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
@Retry
public class SecondService {
    public String publicHello() {
        return "hello";
    }

    private String privateHello() {
        return "hello";
    }
}
