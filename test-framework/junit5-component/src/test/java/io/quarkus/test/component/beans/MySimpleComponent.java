package io.quarkus.test.component.beans;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MySimpleComponent {

    private String name;

    @PostConstruct
    void init() {
        name = "foo";
    }

    public String ping() {
        return name;
    }

}
