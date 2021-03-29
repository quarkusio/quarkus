package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

@Path("/")
public class GenericEntityResource {
    @Path("floats")
    @GET
    public Response getFloats() {
        ArrayList<Float> list = new ArrayList<Float>();
        list.add(45.0f);
        list.add(50.0f);
        GenericEntity<List<Float>> ge = new GenericEntity<List<Float>>(list) {
        };
        return Response.ok(ge).build();
    }

    @Path("doubles")
    @GET
    public Response getDoubles() {
        ArrayList<Double> list = new ArrayList<Double>();
        list.add(45.0);
        list.add(50.0);
        GenericEntity<List<Double>> ge = new GenericEntity<List<Double>>(list) {
        };
        return Response.ok(ge).build();
    }

    @Path("integers")
    @GET
    public Response getIntegers() {
        List<Integer> list = new ArrayList<>();
        list.add(45);
        list.add(50);
        GenericEntity<List<Integer>> ge = new GenericEntity<List<Integer>>(list) {
        };
        return Response.ok(ge).build();
    }

    @Path("integers-no-response")
    @GET
    public List<Integer> getIntegersNoResponse() {
        List<Integer> list = new ArrayList<>();
        list.add(45);
        list.add(50);
        return list;
    }
}
