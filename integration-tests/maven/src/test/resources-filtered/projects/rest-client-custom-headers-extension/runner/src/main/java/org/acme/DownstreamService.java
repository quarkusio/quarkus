package org.acme;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/downstream")
public class DownstreamService {

    @GET
    public String getHeaders(@HeaderParam("CustomHeader1") String customHeader1, @HeaderParam("CustomHeader2") String customHeader2) {
        return customHeader1 + " " + customHeader2;
    }
}
