package io.quarkus.arc.test.context.optimized;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class SimpleBean {

    public boolean ping() {
        return true;
    }

}