package io.quarkus.smallrye.reactivemessaging.amqp.devmode;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/last")
public class TestResource {

    @Inject
    ConsumingBean bean;

    @GET
    public int getLast() {
        return bean.get();
    }

}
