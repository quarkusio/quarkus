package io.quarkus.it.micrometer.prometheus;

import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.micrometer.core.instrument.MeterRegistry;

@Path("/prime")
public class PrimeNumberResource {

    private final LongAccumulator highestPrime = new LongAccumulator(Long::max, 0);
    private final MeterRegistry registry;

    PrimeNumberResource(MeterRegistry registry) {
        this.registry = registry;

        // Create a gauge that uses the highestPrimeNumberSoFar method
        // to obtain the highest observed prime number
        registry.gauge("prime.number.max", this,
                PrimeNumberResource::highestObservedPrimeNumber);
    }

    @GET
    @Path("/{number}")
    @Produces("text/plain")
    public String checkIfPrime(@PathParam("number") long number) {
        if (number < 1) {
            return "Only natural numbers can be prime numbers.";
        }
        if (number == 1) {
            return "1 is not prime.";
        }
        if (number == 2) {
            return "2 is prime.";
        }
        if (number % 2 == 0) {
            return number + " is not prime, it is divisible by 2.";
        }

        Supplier<String> supplier = () -> {
            for (int i = 3; i < Math.floor(Math.sqrt(number)) + 1; i = i + 2) {
                if (number % i == 0) {
                    return number + " is not prime, is divisible by " + i + ".";
                }
            }
            highestPrime.accumulate(number);
            return number + " is prime.";
        };

        return registry.timer("prime.number.test").wrap(supplier).get();
    }

    /**
     * This method is called by the registered {@code highest.prime.number} gauge.
     *
     * @return the highest observed prime value
     */
    long highestObservedPrimeNumber() {
        return highestPrime.get();
    }
}
