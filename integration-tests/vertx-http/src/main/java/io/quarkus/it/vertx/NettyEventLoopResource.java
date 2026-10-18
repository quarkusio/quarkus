package io.quarkus.it.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.netty.channel.EventLoopGroup;
import io.quarkus.netty.BossEventLoopGroup;
import io.quarkus.netty.MainEventLoopGroup;

@Path("/eventloop")
public class NettyEventLoopResource {

    @Inject
    @MainEventLoopGroup
    EventLoopGroup worker;

    @Inject
    @BossEventLoopGroup
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
