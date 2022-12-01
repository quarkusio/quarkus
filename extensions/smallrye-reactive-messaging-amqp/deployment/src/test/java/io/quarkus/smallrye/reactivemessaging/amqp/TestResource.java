package io.quarkus.smallrye.reactivemessaging.amqp;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/last")
public class TestResource {

    @Inject
    ConsumingBean bean;

    @GET
    public long getLast() {
        return bean.get();
    }

}
