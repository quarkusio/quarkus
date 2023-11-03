package io.quarkus.smallrye.reactivemessaging.amqp;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/last")
public class TestResource {

    @Inject
    ConsumingBean bean;

    @GET
    public long getLast() {
        return bean.get();
    }

}
