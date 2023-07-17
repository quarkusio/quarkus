package org.acme;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/downstream")
public class DownstreamService {

    @GET
    public String getHeaders(@HeaderParam("CustomHeader1") String customHeader1, @HeaderParam("CustomHeader2") String customHeader2) {
        return customHeader1 + " " + customHeader2;
    }
}
