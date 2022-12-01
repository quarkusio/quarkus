package io.quarkus.jaeger.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class ZipkinReporterProvider {
    @Produces
    @Singleton
    public ReporterFactory reporter() {
        return new ZipkinReporterFactoryImpl();
    }
}
