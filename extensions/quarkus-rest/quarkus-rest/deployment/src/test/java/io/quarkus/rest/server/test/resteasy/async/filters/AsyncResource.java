package io.quarkus.rest.server.test.resteasy.async.filters;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
