//DEPS io.quarkus:quarkus-bom:${q.v}@pom
//DEPS io.quarkus:quarkus-resteasy
//JAVAC_OPTIONS -parameters

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import java.util.function.Consumer;

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
        ApplicationLifecycleManager.setDefaultExitCodeHandler(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println("Forced exit!");
                System.exit(integer);
            }
        });
        Quarkus.asyncExit(77);
        return "KILLED";
    }
}
