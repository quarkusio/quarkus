package org.jboss.shamrock.example;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/test")
public class TestResource {

    @GET
    public String getTest() {
        return "TEST";
    }

    @GET
    @Path("/json")
    @Produces("application/json")
    public MyData get() {
        MyData m = new MyData();
        m.setName("Stuart Douglas");
        m.setValue("A value");
        return m;
    }

    @GET
    @Path("/jsonp")
    public JsonObject jsonp() {
        return Json.createObjectBuilder()
                .add("name", "Stuart")
                .add("value", "A Value")
                .build();
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
