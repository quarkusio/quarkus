package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.ScheduledExecution
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("scheduled")
class ScheduledEndpoint {

    private val num1 = AtomicInteger(0)
    private val num2 = AtomicInteger(0)

    @Scheduled(every = "0.5s")
    suspend fun scheduled1() {
        delay(100)
        num1.compareAndSet(0, 1)
    }

    @Scheduled(every = "0.5s")
    suspend fun scheduled2(scheduledExecution: ScheduledExecution) {
        delay(100)
        num2.compareAndSet(0, 1)
    }

    @Path("num1")
    @GET
    fun num1() = Response.status(200 + num1.get()).build()

    @Path("num2")
    @GET
    fun num2() = Response.status(200 + num2.get()).build()
}
