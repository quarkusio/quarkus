package org.acme.logging.json;

import org.jboss.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/logging-json")
public class LoggingJsonResource {

    private static final Logger LOG = Logger.getLogger(LoggingJsonResource.class);
    private static final int SPEED_OF_SOUND_IN_METER_PER_SECOND = 343;

    private final AtomicInteger speed = new AtomicInteger(0);
    private final Random random = new Random();

    @GET
    @Path("faster")
    @Produces(MediaType.TEXT_PLAIN)
    public String faster() {
        final int s = speed.addAndGet(random.nextInt(200));
        if (s > SPEED_OF_SOUND_IN_METER_PER_SECOND) {
            throw new ServerErrorException("💥 SONIC BOOOOOM!!!", Response.Status.SERVICE_UNAVAILABLE);
        }
        String message = String.format("Your jet aircraft speed is %s m/s.", s);
        LOG.info(message);
        return message + " Watch the logs...";
    }
}
