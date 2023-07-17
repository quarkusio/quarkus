package io.quarkus.arc.test.exclude;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.test.exclude.ExcludeTypesTest.Pong;

@ApplicationScoped
class Bravo implements Pong {

    public String ping() {
        return "bravo";
    }

}
