package io.quarkus.signals.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.signals.Signal;
import io.smallrye.mutiny.Uni;

@Path("/signals")
public class SignalsResource {

    @Inject
    Signal<HelloName> signal;

    @Inject
    HelloMessageLogger messageLogger;

    @GET
    @Path("{name}")
    public Uni<String> hello(@RestPath String name) {
        return signal.reactive().request(new HelloName(name), HelloMessage.class).map(HelloMessage::text);
    }

    @GET
    @Path("count")
    public int count() {
        return messageLogger.getCount();
    }

}
