package io.quarkus.it.prodmode;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/production-mode-tests")
public class ProductionModeTestsEndpoint {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("areExpectedSystemPropertiesSet")
    @Produces(MediaType.TEXT_PLAIN)
    public String areExpectedSystemPropertiesSet() {
        if (!"org.jboss.logmanager.LogManager".equals(System.getProperty("java.util.logging.manager"))) {
            return "no";
        }
        if (!"io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory"
                .equals(System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory"))) {
            return "no";
        }
        return "yes";
    }

    @GET
    @Path("isForkJoinPoolUsingExpectedClassloader")
    @Produces(MediaType.TEXT_PLAIN)
    public String isForkJoinPoolUsingExpectedClassloader() {
        return ForkJoinPoolAssertions.isEachFJThreadUsingQuarkusClassloader() ? "yes" : "no";
    }

}
