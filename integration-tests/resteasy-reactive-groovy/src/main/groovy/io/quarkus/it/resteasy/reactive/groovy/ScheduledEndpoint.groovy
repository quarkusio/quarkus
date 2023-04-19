package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.ScheduledExecution
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Path("scheduled")
class ScheduledEndpoint {

    private AtomicInteger num1 = new AtomicInteger(0)
    private AtomicInteger num2 = new AtomicInteger(0)

    @Scheduled(every = "0.5s")
    Uni<Void> scheduled1() {
        Uni.createFrom().nullItem()
            .onItem().delayIt().by(Duration.ofMillis(100))
            .onItem().invoke {
                num1.compareAndSet(0, 1)
            }
            .replaceWithVoid()
    }

    @Scheduled(every = "0.5s")
    Uni<Void> scheduled2(ScheduledExecution scheduledExecution) {
        Uni.createFrom().nullItem()
            .onItem().delayIt().by(Duration.ofMillis(100))
            .onItem().invoke {
                num2.compareAndSet(0, 1)
            }
            .replaceWithVoid()
    }

    @Path("num1") @GET def num1() { Response.status(200 + num1.get()).build() }

    @Path("num2") @GET def num2() { Response.status(200 + num2.get()).build() }
}
