package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Outgoing

import java.time.Duration

@CompileStatic
@ApplicationScoped
class CountryNameMessageTransformer {

    @Incoming("countries-t1-in")
    @Outgoing("countries-t2-out")
    Uni<Message<String>> transform(Message<String> input) {
        Uni.createFrom().item(input.withPayload(input.payload.toLowerCase()))
                .onItem().delayIt().by(Duration.ofMillis(100))
    }
}
