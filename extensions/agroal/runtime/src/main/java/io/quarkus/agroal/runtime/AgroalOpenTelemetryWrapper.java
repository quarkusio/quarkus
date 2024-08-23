package io.quarkus.agroal.runtime;

import java.util.function.Function;

import io.agroal.api.AgroalDataSource;

public class AgroalOpenTelemetryWrapper implements Function<AgroalDataSource, AgroalDataSource> {

    @Override
    public AgroalDataSource apply(AgroalDataSource originalDataSource) {
        return new OpenTelemetryAgroalDataSource(originalDataSource);
    }
}
