package io.quarkus.it.resteasy.reactive.generics;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
