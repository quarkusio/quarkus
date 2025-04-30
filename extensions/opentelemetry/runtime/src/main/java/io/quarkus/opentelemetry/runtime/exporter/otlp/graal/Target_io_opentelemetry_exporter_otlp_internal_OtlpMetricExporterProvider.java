package io.quarkus.opentelemetry.runtime.exporter.otlp.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.opentelemetry.exporter.otlp.internal.OtlpMetricExporterProvider")
@Delete
public final class Target_io_opentelemetry_exporter_otlp_internal_OtlpMetricExporterProvider {
}
