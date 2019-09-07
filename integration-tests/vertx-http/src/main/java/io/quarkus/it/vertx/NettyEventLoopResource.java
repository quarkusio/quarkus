package io.quarkus.it.vertx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.netty.channel.EventLoopGroup;

@Path("/eventloop")
public class NettyEventLoopResource {

    //    @Inject
    //    @MainEventLoopGroup
    EventLoopGroup worker;

    //    @Inject
    //    @BossEventLoopGroup
    EventLoopGroup boss;

    @GET
    public String test() {
        if (boss == null) {
            throw new RuntimeException("Boss group null");
        }
        if (worker == null) {
            throw new RuntimeException("worker group null");
        }
        return "passed";
    }
}
