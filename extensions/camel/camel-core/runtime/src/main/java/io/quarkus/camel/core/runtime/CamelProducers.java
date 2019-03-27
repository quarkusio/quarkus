package io.quarkus.camel.core.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class CamelProducers {

    CamelRuntime camelRuntime;

    @Produces
    public CamelRuntime getCamelRuntime() {
        return camelRuntime;
    }

    public void setCamelRuntime(CamelRuntime camelRuntime) {
        this.camelRuntime = camelRuntime;
    }
}
