package io.quarkus.it.micrometer.jmx;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

@Path("/message")
public class MessageResource {

    private final MeterRegistry registry;
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    public MessageResource(MeterRegistry registry) {
        this.registry = registry;
    }

    @GET
    public String message() {
        CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry) registry;
        Set<MeterRegistry> subRegistries = compositeMeterRegistry.getRegistries();
        return subRegistries.iterator().next().getClass().getName();
    }

    @GET
    @Path("fail")
    public String fail() {
        throw new RuntimeException("Failed on purpose");
    }

    @GET
    @Path("item/{id}")
    public String item(@PathParam("id") String id) {
        return "return message with id " + id;
    }

    @GET
    @Path("mbeans")
    public String metrics() throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        Set<ObjectName> mbeans = mBeanServer.queryNames(null, null);
        StringBuilder sb = new StringBuilder();
        for (ObjectName mbean : mbeans) {
            if (mbean.getCanonicalName().startsWith("metrics:name=http")) {
                sb.append(mbean.getCanonicalName()).append("\n");
            }
        }
        return sb.toString();
    }
}
