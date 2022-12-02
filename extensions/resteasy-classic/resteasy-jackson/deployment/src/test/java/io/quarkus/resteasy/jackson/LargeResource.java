package io.quarkus.resteasy.jackson;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/large")
public class LargeResource {

    @GET
    @Path("/bufmult")
    public Map<String, String> hello() {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < 830; ++i) {
            if (i == 0) {
                //hack to make this exactly 2 * 8191 bytes long, as tested by trial and error
                ret.put("key00", "value" + i);
            } else {
                ret.put("key" + i, "value" + i);
            }
        }
        return ret;
    }

    @GET
    @Path("/huge")
    public Map<String, String> huge() {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < 1280; ++i) {
            ret.put("key" + i, "value" + i);
        }
        return ret;
    }
}
