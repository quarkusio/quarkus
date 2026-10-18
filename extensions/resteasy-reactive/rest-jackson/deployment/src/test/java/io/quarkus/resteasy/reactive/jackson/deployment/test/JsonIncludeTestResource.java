package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.smallrye.common.annotation.NonBlocking;

@Path("/json-include")
@NonBlocking
@DisableSecureSerialization
public class JsonIncludeTestResource {

    @GET
    @Path("/my-object-empty")
    public MyObject getEmptyObject() {
        return new MyObject();
    }

    @GET
    @Path("/my-object")
    public MyObject getObject() {
        MyObject myObject = new MyObject();
        myObject.setName("name");
        myObject.setDescription("description");
        myObject.setStrings("test");
        myObject.getMap().put("test", 1);
        return myObject;
    }
}
