package io.quarkus.arc.test.clientproxy.toString;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Foo {
    // class deliberately doesn't override toString() as in such case it would be always invoked in contextual instance
}
