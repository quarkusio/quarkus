package io.quarkus.panache.mock;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyComponent {

    public long ping() {
        return Person.count();
    }

}
