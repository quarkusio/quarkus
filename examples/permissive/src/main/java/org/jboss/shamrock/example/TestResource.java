package org.jboss.shamrock.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metric;

@Path("/test")
public class TestResource {

    @GET
    @Counted(monotonic = true)
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
