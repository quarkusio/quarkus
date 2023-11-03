package io.quarkus.jaeger.runtime;

import io.jaegertracing.spi.Reporter;
import io.jaegertracing.zipkin.ZipkinV2Reporter;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

public class ZipkinReporterFactoryImpl implements ReporterFactory {

    public Reporter createReporter(String endpoint) {

        return new ZipkinV2Reporter(AsyncReporter.create(URLConnectionSender.create(endpoint)));

    }

}
