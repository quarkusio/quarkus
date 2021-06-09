package io.quarkus.it.rest.client.main;

import io.quarkus.runtime.annotations.RegisterForReflection;

// TODO get rid of need for this
@RegisterForReflection
public class Apple {
    private String cultivar;

    public Apple() {
    }

    public Apple(String cultivar) {
        this.cultivar = cultivar;
    }

    public String getCultivar() {
        return cultivar;
    }

    public void setCultivar(String cultivar) {
        this.cultivar = cultivar;
    }
}
