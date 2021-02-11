package io.quarkus.arc.test.interceptors.no.priority;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AverageBean {

    @Simple
    public void ping() {

    }
}
