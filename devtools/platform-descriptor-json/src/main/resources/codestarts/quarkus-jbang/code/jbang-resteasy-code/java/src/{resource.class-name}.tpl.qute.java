//usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS xamdk=https://xam.dk/maven
{#for dep in dependencies}
//DEPS {dep.formatted-ga}:{quarkus.version}
{/for}

//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//Q:CONFIG quarkus.swagger-ui.always-include=true

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