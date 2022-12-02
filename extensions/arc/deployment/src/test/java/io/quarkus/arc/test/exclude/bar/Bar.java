package io.quarkus.arc.test.exclude.bar;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.test.exclude.ExcludeTypesTest.Pong;

@ApplicationScoped
public class Bar implements Pong {

    @Override
    public String ping() {
        return "bar";
    }

}
