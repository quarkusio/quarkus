package io.quarkus.agroal.runtime;

import java.util.function.Function;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AgroalOpenTelemetryRecorder {

    /**
     * Tell {@link DataSources} to use the OpenTelemetry datasource wrapper for
     * data sources with activated telemetry. We need to set this way only when
     * optional OpenTelemetry JDBC instrumentation dependency is present to avoid native failures.
     */
    public void prepareOpenTelemetryAgroalDatasource() {
        DataSources.setOpenTelemetryDatasourceTransformer(new Function<AgroalDataSource, AgroalDataSource>() {
            @Override
            public AgroalDataSource apply(AgroalDataSource agroalDataSource) {
                return new OpenTelemetryAgroalDataSource(agroalDataSource);
            }
        });
    }
}
