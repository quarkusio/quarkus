package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
