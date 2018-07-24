package org.jboss.shamrock.example;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;

@Path("/test")
public class TestResource {

    @GET
    public String getTest() {
        return "TEST";
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
