package io.quarkus.camel.core.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;

@ApplicationScoped
public class CamelProducers {

    CamelRuntime camelRuntime;

    @Produces
    public CamelContext getCamelContext() {
        return camelRuntime.getContext();
    }

    @Produces
    public Registry getCamelRegistry() {
        return camelRuntime.getRegistry();
    }

    @Produces
    public CamelConfig.BuildTime getCamelBuildTimeConfig() {
        return camelRuntime.getBuildTimeConfig();
    }

    @Produces
    public CamelConfig.Runtime getCamelRuntimeConfig() {
        return camelRuntime.getRuntimeConfig();
    }

    public void setCamelRuntime(CamelRuntime camelRuntime) {
        this.camelRuntime = camelRuntime;
    }

}
