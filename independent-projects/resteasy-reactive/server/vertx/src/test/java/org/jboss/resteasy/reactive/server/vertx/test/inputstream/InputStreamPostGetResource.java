package org.jboss.resteasy.reactive.server.vertx.test.inputstream;

import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;

@Path("/inputstreamtransfer")
public class InputStreamPostGetResource {
    private static Logger LOG = Logger.getLogger(InputStreamPostGetResource.class);

    @POST
    @Path("/test")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Blocking
    public String test(InputStream is) throws IOException {
        int read;
        long size = 0;
        byte[] b = new byte[65536];
        while ((read = is.read(b)) > 0) {
            size += read;
        }
        LOG.infof("Read %d", size);
        return Long.toString(size);
    }

    @GET
    @Path("/test/{len}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Blocking
    public InputStream test(@PathParam("len") long len) throws IOException {
        LOG.infof("To Write %d", len);
        return new FakeInputStream(len);
    }

}
