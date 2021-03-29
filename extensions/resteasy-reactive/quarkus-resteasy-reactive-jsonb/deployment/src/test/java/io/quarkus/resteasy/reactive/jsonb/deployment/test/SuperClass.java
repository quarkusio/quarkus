package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class SuperClass<T> {

    @POST
    @Path("/super")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<T> reverseListFromSuper(List<T> list) {
        List<T> reversed = new ArrayList<>(list.size());
        for (T t : list) {
            reversed.add(0, t);
        }
        return reversed;
    }
}
