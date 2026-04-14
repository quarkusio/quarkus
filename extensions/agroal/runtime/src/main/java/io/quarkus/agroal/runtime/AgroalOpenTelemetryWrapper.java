package io.quarkus.agroal.runtime;

import jakarta.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.OpenTelemetryDataSource;

public class AgroalOpenTelemetryWrapper {

    @Inject
    OpenTelemetry openTelemetry;

    public AgroalDataSource wrap(AgroalDataSource originalDataSource,
            DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig) {
        OpenTelemetryDataSource otelDataSource = (OpenTelemetryDataSource) JdbcTelemetry
                .builder(openTelemetry)
                .setDataSourceInstrumenterEnabled(dataSourceJdbcRuntimeConfig.telemetryTraceConnection())
                .build()
                .wrap(originalDataSource);
        return new OpenTelemetryAgroalDataSource(originalDataSource, otelDataSource);
    }
}
