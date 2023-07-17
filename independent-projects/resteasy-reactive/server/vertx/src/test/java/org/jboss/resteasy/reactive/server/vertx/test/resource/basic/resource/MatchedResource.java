package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

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
