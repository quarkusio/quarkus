package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
