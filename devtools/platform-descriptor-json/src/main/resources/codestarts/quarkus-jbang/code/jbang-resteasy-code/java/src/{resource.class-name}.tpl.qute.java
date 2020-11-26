//usr/bin/env jbang "$0" "$@" ; exit $?
{#for dep in dependencies}
//DEPS {dep.formatted-ga}:{quarkus.version}
{/for}

//JAVAC_OPTIONS -parameters

import io.quarkus.runtime.Quarkus;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("{resource.path}")
@ApplicationScoped
public class {resource.class-name} {

    @GET
    public String sayHello() {
        return "{resource.response}";
    }

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}