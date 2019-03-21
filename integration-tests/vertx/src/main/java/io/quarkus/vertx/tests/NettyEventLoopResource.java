package io.quarkus.vertx.tests;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.netty.channel.EventLoopGroup;
import io.quarkus.netty.BossGroup;

@Path("/eventloop")
public class NettyEventLoopResource {

    @Inject
    EventLoopGroup worker;

    @Inject
    @BossGroup
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
