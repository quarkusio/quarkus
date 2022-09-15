package org.jboss.resteasy.reactive.server.vertx.test.resteasy.async.filters;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/async")
public class AsyncResource {
    @GET
    @Produces("text/plain")
    public void get(@Suspended final AsyncResponse response) {
        response.setTimeout(2000, TimeUnit.MILLISECONDS);
        Thread t = new Thread() {
            private Logger log = Logger.getLogger(AsyncResource.class);

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    Response jaxrs = Response.ok("hello").type(MediaType.TEXT_PLAIN).build();
                    response.resume(jaxrs);
                } catch (Exception e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    log.error(errors.toString());
                }
            }
        };
        t.start();
    }

    @GET
    @Path("timeout")
    @Produces("text/plain")
    public void timeout(@Suspended final AsyncResponse response) {
        response.setTimeout(10, TimeUnit.MILLISECONDS);
        Thread t = new Thread() {
            private Logger log = Logger.getLogger(AsyncResource.class);

            @Override
            public void run() {
                try {
                    Thread.sleep(100000);
                    Response jaxrs = Response.ok("goodbye").type(MediaType.TEXT_PLAIN).build();
                    response.resume(jaxrs);
                } catch (Exception e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    log.error(errors.toString());
                }
            }
        };
        t.start();
    }
}
