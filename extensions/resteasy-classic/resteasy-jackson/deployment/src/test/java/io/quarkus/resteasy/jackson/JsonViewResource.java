package io.quarkus.resteasy.jackson;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonView;

@Produces(MediaType.APPLICATION_JSON)
@Path("/json-view")
public class JsonViewResource {

    @GET
    @JsonView(View1.class)
    @Path("/view1")
    public MyObject view1() {
        return new MyObject("value1", "value2");
    }

    @GET
    @JsonView(View2.class)
    @Path("/view2")
    public MyObject view2() {
        return new MyObject("value1", "value2");
    }

    public static class MyObject {

        @JsonView(View1.class)
        private String property1;

        @JsonView(View2.class)
        private String property2;

        MyObject() {
        }

        MyObject(String property1, String property2) {
            this.property1 = property1;
            this.property2 = property2;
        }
    }

    public static class View1 {
    }

    public static class View2 {
    }
}
