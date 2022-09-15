package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.junit.jupiter.api.Assertions;

@Path("/collection")
public class CollectionDefaultValueResource {

    private static class MyParams {
        @QueryParam("nada")
        List<String> params;
        @DefaultValue("foo")
        @QueryParam("nada")
        List<String> paramsWithDefaultValue;
    }

    @BeanParam
    MyParams params;

    @GET
    @Produces("text/plain")
    public String get(@QueryParam("nada") List<String> params,
            @DefaultValue("foo") @QueryParam("nada") List<String> paramsWithDefaultValue) {
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.size());
        Assertions.assertNotNull(paramsWithDefaultValue);
        Assertions.assertEquals(1, paramsWithDefaultValue.size());
        Assertions.assertEquals("foo", paramsWithDefaultValue.get(0));
        Assertions.assertNotNull(this.params.params);
        Assertions.assertEquals(0, this.params.params.size());
        Assertions.assertNotNull(this.params.paramsWithDefaultValue);
        Assertions.assertEquals(1, this.params.paramsWithDefaultValue.size());
        Assertions.assertEquals("foo", this.params.paramsWithDefaultValue.get(0));
        return "hello";
    }

}
