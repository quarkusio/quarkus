package io.quarkus.it.opentelemetry.vertx;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.MultiMap;

@ApplicationScoped
public class EventBusConsumer {

    @ConsumeEvent("pets")
    // non-blocking
    public String sayHi(Pet pet) {
        Log.infov("Received a pet: {0} {1}", pet, Span.current());
        process();
        return "Hello " + pet.getName() + " (" + pet.getKind() + ")";
    }

    @ConsumeEvent("persons")
    @Blocking
    public String name(String name) {
        Log.infov("Received a pet: {0} {1}", name, Span.current());
        process();
        return "Hello " + name;
    }

    @ConsumeEvent("person-headers")
    @RunOnVirtualThread
    public String personWithHeader(MultiMap headers, Person person) {
        Log.infov("Received a person: {0} {1}", person, Span.current());
        process();
        String s = "Hello " + person.getFirstName() + " " + person.getLastName() + ", " + headers;
        return s;
    }

    @WithSpan
    public void process() {

    }

}
