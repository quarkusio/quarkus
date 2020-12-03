package io.quarkus.reactivemessaging.http;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
@Path("http-helper")
public class HttpTestHelper {
    private final List<String> messages = new ArrayList<>();

    @Channel("http-sink")
    Emitter<String> emitter;

    @Incoming("http-source")
    void consume(String message) {
        messages.add(message);
    }

    @POST
    public void add(String message) {
        emitter.send(message);
    }

    @GET
    public String getMessages() {
        return String.join(",", messages);
    }

    @DELETE
    public void clear() {
        messages.clear();
    }
}
