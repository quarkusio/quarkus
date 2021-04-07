package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import javax.enterprise.context.ApplicationScoped;

public interface HelloService {
    String name();

    @IfBuildProfile("foo")
    @ApplicationScoped
    class HelloServiceFoo implements HelloService{

        @Override
        public String name() {
            return "from foo";
        }
    }

    @IfBuildProfile("bar")
    @ApplicationScoped
    class HelloServiceBar implements HelloService{

        @Override
        public String name() {
            return "from bar";
        }
    }
}
