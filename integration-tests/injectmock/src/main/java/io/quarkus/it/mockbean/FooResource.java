package io.quarkus.it.mockbean;

import java.util.Collections;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("foo")
public class FooResource {

    private final FooService fooService;

    public FooResource(FooService fooService) {
        this.fooService = fooService;
    }

    @GET
    @Path("/{name}/{count}/{barName}")
    public Foo newFoo(@PathParam("name") String name, @PathParam("count") int count, @PathParam("barName") String barName) {
        return fooService.newFoo(name).count(count).bar(new Foo.Bar(Collections.singletonList(barName))).build();
    }
}
