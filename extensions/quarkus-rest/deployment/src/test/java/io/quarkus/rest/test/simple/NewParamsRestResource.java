package io.quarkus.rest.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.quarkus.rest.RestCookie;
import io.quarkus.rest.RestForm;
import io.quarkus.rest.RestHeader;
import io.quarkus.rest.RestMatrix;
import io.quarkus.rest.RestPath;
import io.quarkus.rest.RestQuery;

@Path("/new-params/{klass}/{regex:[^/]+}")
public class NewParamsRestResource {

    @GET
    @Path("{id}")
    public String get(String klass, String regex, String id) {
        return "GET:" + klass + ":" + regex + ":" + id;
    }

    @POST
    @Path("params/{p}")
    public String params(@RestPath String p,
            @RestQuery String q,
            @RestHeader int h,
            @RestForm String f,
            @RestMatrix String m,
            @RestCookie String c) {
        return "params: p: " + p + ", q: " + q + ", h: " + h + ", f: " + f + ", m: " + m + ", c: " + c;
    }

}
