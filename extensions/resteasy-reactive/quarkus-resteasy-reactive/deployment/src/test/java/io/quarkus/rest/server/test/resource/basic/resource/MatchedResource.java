package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class MatchedResource {
    @Path("/test1/{id}.xml.{lang}")
    @GET
    public String getComplex() {
        return "complex";
    }

    @Path("/test1/{id}")
    @GET
    public String getSimple() {
        return "simple";
    }

    @Path("/test2/{id}")
    @GET
    public String getSimple2() {
        return "simple2";
    }

    @Path("/test2/{id}.xml.{lang}")
    @GET
    public String getComplex2() {
        return "complex2";
    }

    @Path("match")
    @Produces("*/*;qs=0.0")
    @GET
    public String getObj() {
        return "*/*";
    }

    @Path("match")
    @Produces("application/xml")
    @GET
    public String getObjXml() {
        return "<xml/>";
    }

    @Path("match")
    @Produces("application/json")
    @GET
    public String getObjJson() {
        return "{ \"name\" : \"bill\" }";
    }

    @Path("start")
    @POST
    @Produces("text/plain")
    public String start() {
        return "started";
    }

    @Path("start")
    @Consumes("application/xml")
    @POST
    @Produces("text/plain")
    public String start(String xml) {
        return xml;
    }

}
