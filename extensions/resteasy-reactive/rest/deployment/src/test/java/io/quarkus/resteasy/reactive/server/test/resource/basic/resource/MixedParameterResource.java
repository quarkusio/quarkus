package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/MixedParameterResource")
public class MixedParameterResource {

    @POST
    @Path("/mixed")
    public String mixedParam(@OtherAnnotation MyOtherObject otherObject, @QueryParam("foo") String query, String body) {
        return query.concat(".").concat(body);
    }

    @GET
    @Path("/single")
    public String singleParam(@OtherAnnotation MyOtherObject otherObject) {
        return "ok";
    }

    public static class MyOtherObject {
        String param;

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.PARAMETER })
    public @interface OtherAnnotation {
    }
}
