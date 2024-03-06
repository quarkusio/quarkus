package ilove.quark.us;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from RESTEasy Reactive";
    }

    // Use in IDE: Starts the app for development. Not used in production.
    public static void main(String... args) { io.quarkus.runtime.Quarkus.run(args); }

}
