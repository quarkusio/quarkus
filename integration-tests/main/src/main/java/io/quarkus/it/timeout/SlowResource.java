package io.quarkus.it.timeout;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/test/slow")
public class SlowResource {

    @GET
    public String manual() throws Exception {
        Thread.sleep(5000);
        return "never anwser";
    }
}
