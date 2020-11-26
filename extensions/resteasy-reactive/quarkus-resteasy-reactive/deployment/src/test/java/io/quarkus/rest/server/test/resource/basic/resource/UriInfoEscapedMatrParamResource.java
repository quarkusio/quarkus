package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

@Path("UriInfoEscapedMatrParamResource/queryEscapedMatrParam")
public class UriInfoEscapedMatrParamResource {
    private static final String ERROR_MSG = "Wrong parameter";

    @GET
    public String doGet(@MatrixParam("a") String a, @MatrixParam("b") String b, @MatrixParam("c") String c,
            @MatrixParam("d") String d) {
        Assertions.assertEquals(ERROR_MSG, "a;b", a);
        Assertions.assertEquals(ERROR_MSG, "x/y", b);
        Assertions.assertEquals(ERROR_MSG, "m\\n", c);
        Assertions.assertEquals(ERROR_MSG, "k=l", d);
        return "content";
    }
}
