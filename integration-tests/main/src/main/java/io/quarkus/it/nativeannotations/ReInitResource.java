package io.quarkus.it.nativeannotations;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/class-reinit")
public class ReInitResource {

    @GET
    public String result() {
        //ReinitClass should have got a new timestamp when it was runtime re-initialized
        if (ReinitClass.timestamp == StaticTimeClass.staticTime) {
            throw new RuntimeException("Failed");
        }
        return "passed";
    }

}
