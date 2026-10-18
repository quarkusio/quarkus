package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FooService {

    public Foo.FooBuilder newFoo(String name) {
        return new Foo.FooBuilder(name);
    }

}
