//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus:quarkus-bom:999-MOCK@pom
//DEPS io.quarkus:quarkus-resteasy

import io.quarkus.runtime.Quarkus;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/hello-resteasy")
@ApplicationScoped
public class main {

    @GET
    public String sayHello() {
        return "Hello RESTEasy";
    }

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}