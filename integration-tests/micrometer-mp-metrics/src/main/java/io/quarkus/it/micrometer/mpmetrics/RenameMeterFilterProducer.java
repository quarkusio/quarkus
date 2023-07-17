package io.quarkus.it.micrometer.mpmetrics;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;

@Singleton
public class RenameMeterFilterProducer {
    static String targetMetric = PrimeResource.class.getName() + ".highestPrimeNumberSoFar";

    @Produces
    MeterFilter renameMeterFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().equals(targetMetric)) {
                    List<Tag> tags = id.getTags().stream().filter(x -> !"scope".equals(x.getKey()))
                            .collect(Collectors.toList());
                    return id.withName("highestPrimeNumberSoFar").replaceTags(tags);
                }
                return id;
            }
        };
    }
}
