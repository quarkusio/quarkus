package io.quarkus.smallrye.metrics.test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

@Path("/")
public class MetricResource {

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry vendorRegistry;

    @Inject
    @RegistryType(type = MetricRegistry.Type.BASE)
    MetricRegistry baseRegistry;

    @GET
    @Path("/get-counters")
    @Produces("application/json")
    public String[] getRegisteredCountersInVendorRegistryType() {
        return vendorRegistry.getCounters().keySet().stream().map(MetricID::getName).toArray(String[]::new);
    }

    @GET
    @Path("/get-counters-base")
    @Produces("application/json")
    public String[] getRegisteredCountersInBaseRegistryType() {
        return baseRegistry.getCounters().keySet().stream().map(MetricID::getName).toArray(String[]::new);
    }

    public void countMePlease() {

    }

    private void countMePlease2() {

    }

    public void countMeInBaseScope() {

    }

}
