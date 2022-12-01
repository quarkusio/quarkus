package io.quarkus.arc.test.unused.subpackage;

import javax.enterprise.context.ApplicationScoped;

/**
 * Another unused bean that shouldn't be removed.
 */
@ApplicationScoped
public class Beta {

    public String ping() {
        return "ok";
    }
}
