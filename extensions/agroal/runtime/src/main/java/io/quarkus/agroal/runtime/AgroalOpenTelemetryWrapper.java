package io.quarkus.agroal.runtime;

import java.util.function.Function;

import jakarta.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.OpenTelemetryDataSource;

public class AgroalOpenTelemetryWrapper implements Function<AgroalDataSource, AgroalDataSource> {

    @Inject
    OpenTelemetry openTelemetry;

    @Override
    public AgroalDataSource apply(AgroalDataSource originalDataSource) {
        OpenTelemetryDataSource otelDataSource = (OpenTelemetryDataSource) JdbcTelemetry
                .create(openTelemetry)
                .wrap(originalDataSource);
        return new OpenTelemetryAgroalDataSource(originalDataSource, otelDataSource);
    }
}
