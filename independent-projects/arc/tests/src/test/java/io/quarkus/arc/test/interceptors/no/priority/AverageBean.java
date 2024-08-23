package io.quarkus.arc.test.interceptors.no.priority;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AverageBean {

    @Simple
    public void ping() {

    }
}
