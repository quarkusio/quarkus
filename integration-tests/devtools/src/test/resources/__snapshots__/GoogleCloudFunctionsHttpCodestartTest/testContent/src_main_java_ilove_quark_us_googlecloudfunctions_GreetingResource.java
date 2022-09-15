package ilove.quark.us.googlecloudfunctionshttp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String hello(String name) {
        return "hello " + name;
    }

    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] hello(byte[] bytes) {
        if (bytes[0] != 0 || bytes[1] != 1 || bytes[2] != 2 || bytes[3] != 3) {
            throw new RuntimeException("bad input");
        }
        byte[] rtn = { 4, 5, 6 };
        return rtn;
    }

    @POST
    @Path("empty")
    public void empty() {

    }

    @GET
    @Path("error")
    public void error() {
        throw new RuntimeException("Oups!");
    }

}
