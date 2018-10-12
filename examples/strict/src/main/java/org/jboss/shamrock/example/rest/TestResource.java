package org.jboss.shamrock.example.rest;

import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.xml.bind.annotation.XmlRootElement;

import io.reactivex.Single;

@Path("/test")
public class TestResource {

    @Context
    HttpServletRequest request;

    @GET
    public String getTest() {
        return "TEST";
    }

    @GET
    @Path("/int/{val}")
    public Integer getInt(@PathParam("val") Integer val) {
        return val + 1;
    }

    @GET
    @Path("/request-test")
    public String requestTest() {
        return request.getRequestURI();
    }

    @GET
    @Path("/jackson")
    @Produces("application/json")
    public MyData get() {
        MyData m = new MyData();
        m.setName("Stuart");
        m.setValue("A Value");
        return m;
    }

    @GET
    @Path("/jsonp")
    @Produces("application/json")
    public JsonObject jsonp() {
        return Json.createObjectBuilder()
                .add("name", "Stuart")
                .add("value", "A Value")
                .build();
    }

    @GET
    @Produces("application/xml")
    @Path("/xml")
    public XmlObject xml() {
        XmlObject xmlObject = new XmlObject();
        xmlObject.setValue("A Value");
        return xmlObject;
    }

    @GET
    @Path("/rx")
    public Single<String> rx() {
        return Single.just("Hello");
    }

    @GET
    @Path("/complex")
    @Produces("application/json")
    public List<ComponentType> complex() {
        ComponentType ret = new ComponentType();
        ret.setValue("component value");
        CollectionType ct = new CollectionType();
        ct.setValue("collection type");
        ret.getCollectionTypes().add(ct);
        SubComponent subComponent = new SubComponent();
        subComponent.getData().add("sub component list value");
        ret.setSubComponent(subComponent);
        return Collections.singletonList(ret);

    }

    @XmlRootElement
    public static class XmlObject {

        String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class MyData {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
