package io.quarkus.it.resteasy.jackson.generics;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/generics")
public class GenericsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ExtendedClass testExtendedClass() {
        final ExtendedClass c = new ExtendedClass();
        c.setBaseVariable("myBaseVariable");
        c.setExtendedVariable("myExtendedVariable");
        final MyData d = new MyData();
        d.setDataVariable("myData");
        c.setData(d);
        return c;
    }
}
