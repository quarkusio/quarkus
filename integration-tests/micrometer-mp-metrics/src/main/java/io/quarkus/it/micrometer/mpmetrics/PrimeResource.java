package io.quarkus.it.micrometer.mpmetrics;

import java.util.concurrent.atomic.LongAccumulator;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.*;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/prime")
public class PrimeResource {

    private LongAccumulator highestPrimeSoFar = new LongAccumulator(Long::max, 2);

    @Inject
    CountedInstance countedResource;

    InjectedInstance injectedMetrics;

    @Metered
    PrimeResource(InjectedInstance injectedMetrics) {
        this.injectedMetrics = injectedMetrics;
    }

    @GET
    @Path("/{number}")
    @Produces("text/plain")
    @ConcurrentGauge(description = "active calls to checkIfPrime")
    public String checkIfPrime(@PathParam long number) {
        String result = checkPrime(number);

        if (result.length() > 0) {
            injectedMetrics.count.inc();
            return result;
        }

        countedResource.countPrimes();
        highestPrimeSoFar.accumulate(number);
        return number + " is prime.";
    }

    // This method is both counted and timed. The counted annotation should be suppressed/removed
    @Timed(name = "checksTimer", description = "Measure how long it takes to perform the primality test.", unit = MetricUnits.MILLISECONDS)
    @Counted
    String checkPrime(long number) {
        injectedMetrics.histogram.update(number);

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
        for (int i = 3; i < Math.floor(Math.sqrt(number)) + 1; i = i + 2) {
            if (number % i == 0) {
                return number + " is not prime, is divisible by " + i + ".";
            }
        }
        return "";
    }

    @Gauge(name = "highestPrimeNumberSoFar", unit = MetricUnits.NONE, description = "Highest prime number so far.")
    public Long highestPrimeNumberSoFar() {
        return highestPrimeSoFar.get();
    }

    @Gauge(unit = MetricUnits.NONE, description = "Highest prime number so far.")
    public Long highestPrimeNumberSoFar2() {
        return highestPrimeSoFar.get();
    }
}
