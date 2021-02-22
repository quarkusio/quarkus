package io.quarkus.arc.processor.types;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class Bar {

    @FooQualifier(alpha = "yes", bravo = "no")
    @Inject
    Foo foo;

    @Inject
    Bar(List<String> list, @FooQualifier(alpha = "1", bravo = "2") Foo foo) {
    }

    @Inject
    public void init(Foo foo, List<String> list) {
    }

}
