package io.quarkus.arc.test.exclude.baz.bazzz;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.test.exclude.ExcludeTypesTest.Pong;

@ApplicationScoped
public class Baz implements Pong {

    @Override
    public String ping() {
        return "baz";
    }

}
