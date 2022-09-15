// tag::example[]
// tag::ignore[]
// Source: {{source}}
package io.quarkus.doc.micrometer;

/*
// end::ignore[]
package org.acme.micrometer;

// tag::ignore[]
*/
// end::ignore[]
import java.util.LinkedList;
import java.util.NoSuchElementException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

@Path("/example")
@Produces("text/plain")
public class ExampleResource {
    private final LinkedList<Long> list = new LinkedList<>();
    // tag::registry[]
    private final MeterRegistry registry;

    ExampleResource(MeterRegistry registry) {
        this.registry = registry;
        // tag::gauge[]
        registry.gaugeCollectionSize("example.list.size", Tags.empty(), list);
        // end::gauge[]
    }
    // end::registry[]
    // tag::gauge[]

    @GET
    @Path("gauge/{number}")
    public Long checkListSize(@PathParam("number") long number) {
        if (number == 2 || number % 2 == 0) {
            // add even numbers to the list
            list.add(number);
        } else {
            // remove items from the list for odd numbers
            try {
                number = list.removeFirst();
            } catch (NoSuchElementException nse) {
                number = 0;
            }
        }
        return number;
    }
    // end::gauge[]

    // tag::timed[]
    // tag::counted[]
    @GET
    @Path("prime/{number}")
    public String checkIfPrime(@PathParam("number") long number) {
        if (number < 1) {
            // tag::counter[]
            registry.counter("example.prime.number", "type", "not-natural") // <1>
                    .increment(); // <2>
            // end::counter[]
            return "Only natural numbers can be prime numbers.";
        }
        if (number == 1) {
            // tag::counter[]
            registry.counter("example.prime.number", "type", "one") // <1>
                    .increment(); // <2>
            // end::counter[]
            return number + " is not prime.";
        }
        if (number == 2 || number % 2 == 0) {
            // tag::counter[]
            registry.counter("example.prime.number", "type", "even") // <1>
                    .increment(); // <2>
            // end::counter[]
            return number + " is not prime.";
        }

        // tag::timer[]
        if (timedTestPrimeNumber(number)) { // <3>
            // end::timer[]
            // tag::ignore[]
            /*
             * }
             * // end::ignore[]
             * // tag::default[]
             * if (testPrimeNumber(number)) {
             * // end::default[]
             * // tag::ignore[]
             */
            // end::ignore[]
            // tag::counter[]
            registry.counter("example.prime.number", "type", "prime") // <1>
                    .increment(); // <2>
            // end::counter[]
            return number + " is prime.";
        } else {
            // tag::counter[]
            registry.counter("example.prime.number", "type", "not-prime") // <1>
                    .increment(); // <2>
            // end::counter[]
            return number + " is not prime.";
        }
    }
    // end::counted[]
    // tag::timer[]

    protected boolean timedTestPrimeNumber(long number) {
        Timer.Sample sample = Timer.start(registry); // <4>
        boolean result = testPrimeNumber(number); // <5>
        sample.stop(registry.timer("example.prime.number.test", "prime", result + "")); // <6>
        return result;
    }
    // end::timer[]
    // end::timed[]

    protected boolean testPrimeNumber(long number) {
        for (int i = 3; i < Math.floor(Math.sqrt(number)) + 1; i = i + 2) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}
// end::example[]
