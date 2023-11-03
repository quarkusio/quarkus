package io.quarkus.jaeger.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class ZipkinReporterProvider {
    @Produces
    @Singleton
    public ReporterFactory reporter() {
        return new ZipkinReporterFactoryImpl();
    }
}
