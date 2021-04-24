//DEPS io.quarkus:quarkus-bom:${q.v}@pom
//DEPS io.quarkus:quarkus-resteasy
//JAVAC_OPTIONS -parameters

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import io.quarkus.runtime.ShutdownEvent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static io.quarkus.runtime.LaunchMode.current;

@Path("/hello")
@ApplicationScoped
public class devmode {

    @GET
    public String sayHello() {
        return "Hello from Quarkus with jbang.dev";
    }

    boolean killed = false;

    @Path("/kill")
    @GET
    public String hello() {
        killed = true;
        Quarkus.asyncExit(77);
        return "KILLED";
    }

    void onStop(@Observes ShutdownEvent ev) {
        if(killed && current().equals(LaunchMode.DEVELOPMENT)) {
            //force system exit in case of devmode
            System.out.println("Forced exit!");
            System.exit(77);
        }
    }
}
