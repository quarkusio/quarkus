package io.quarkus.amazon.common.runtime;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.util.Optional;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import software.amazon.awssdk.regions.Region;

/**
 * A converter which converts a string with AWS region, e.g. {@code "eu-central-1"} into
 * an instance of {@link Region}. If an address is given, then a resolved instance is returned.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class RegionConverter implements Converter<Region> {

    @Override
    public Region convert(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        Optional<Region> regionFound = Region.regions().stream()
                .filter(region -> value.equals(region.id()))
                .findAny();

        return regionFound.orElseThrow(
                () -> new IllegalArgumentException(
                        String.format("value %s is not a valid AWS Region %s", value, Region.regions().toString())));
    }
}
