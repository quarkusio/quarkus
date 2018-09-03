package org.jboss.protean.arc.example;

import java.io.Serializable;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

@MyQualifier(alpha = "1", bravo = "1")
@Singleton
public class Foo implements Serializable {

    private Bar bar;

    @Inject
    Instance<Bar> barInstance;

    @Inject
    Foo(Bar bar) {
        this.bar = bar;
    }

    String ping() {
        return bar.getName();
    }

    String lazyPing() {
        return barInstance.isResolvable() ? barInstance.get().getName() : "NOK";
    }

}
