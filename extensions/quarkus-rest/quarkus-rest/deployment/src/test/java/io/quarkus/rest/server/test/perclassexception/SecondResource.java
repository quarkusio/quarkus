package io.quarkus.rest.server.test.perclassexception;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("second")
public class SecondResource {

    @GET
    @Produces("text/plain")
    public String throwsMyException() {
        throw new MyException();
    }
}
