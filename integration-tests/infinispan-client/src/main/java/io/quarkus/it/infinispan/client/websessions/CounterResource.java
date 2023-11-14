package io.quarkus.it.infinispan.client.websessions;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.ext.web.Session;

@Path("/counter")
public class CounterResource {
    @Inject
    Session session;

    @GET
    public String counter() {
        Integer counter = session.get("counter");
        counter = counter == null ? 1 : counter + 1;
        session.put("counter", counter);
        return session.id() + "|" + counter;
    }
}
