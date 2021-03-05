package io.quarkus.resteasy.reactive.server.test.path;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Per spec:
 * <quote>
 * Paths are relative. For an annotated class the base URI is the application path, see ApplicationPath.
 * For an annotated method the base URI is the effective URI of the containing class. For the purposes of
 * absolutizing a path against the base URI , a leading '/' in a path is ignored and base URIs are treated
 * as if they ended in '/'.
 * </quote>
 */
@Path("hello")
public class HelloResource {

    @GET
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/nested")
    public String nested() {
        return "world hello";
    }
}
