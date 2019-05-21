package io.quarkus.it.kogito.drools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.kie.kogito.rules.RuleUnit;
import org.kie.kogito.rules.impl.SessionMemory;

@Path("/candrink/{name}/{age}")
public class CanDrinkResource {

    @Inject
    @Named("canDrinkKS")
    RuleUnit<SessionMemory> ruleUnit;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String canDrink(@PathParam("name") String name, @PathParam("age") int age) {
        SessionMemory memory = new SessionMemory();

        Result result = new Result();
        memory.add(result);
        memory.add(new Person(name, age));

        ruleUnit.evaluate(memory);

        return result.toString();
    }
}
