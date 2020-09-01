package io.quarkus.rest.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.junit.jupiter.api.Assertions;

@Path("/collection")
public class CollectionDefaultValueResource {
    @GET
    @Produces("text/plain")
    public String get(@QueryParam("nada") List<String> params) {
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
        return "hello";
    }

}
