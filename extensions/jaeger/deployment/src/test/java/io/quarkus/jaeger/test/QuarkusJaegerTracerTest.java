package io.quarkus.jaeger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.JaegerTracer.Builder;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.zipkin.ZipkinV2Reporter;
import io.opentracing.Tracer;
import io.quarkus.jaeger.runtime.QuarkusJaegerTracer;
import io.quarkus.jaeger.runtime.ReporterFactory;
import io.quarkus.jaeger.runtime.ZipkinReporterFactoryImpl;

public class QuarkusJaegerTracerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void withzipkinCompatibilityMode() {

        try (MockedStatic<Configuration> mockedStaticConfiguration = Mockito.mockStatic(Configuration.class);
                MockedStatic<CDI> mockedStaticCDI = Mockito.mockStatic(CDI.class)) {

            CDI<Object> mockedCDI = (CDI<Object>) Mockito.mock(CDI.class);

            mockedStaticCDI.when(() -> CDI.current()).thenReturn(mockedCDI);

            Instance instanceCDI = Mockito.mock(Instance.class);
            Mockito.when(instanceCDI.isAmbiguous()).thenReturn(false);
            Mockito.when(instanceCDI.isUnsatisfied()).thenReturn(false);
            Mockito.when(instanceCDI.get()).thenReturn(new ZipkinReporterFactoryImpl());
            Mockito.when(mockedCDI.select(ReporterFactory.class, Default.Literal.INSTANCE)).thenReturn(instanceCDI);

            Configuration mockedInstanceConfiguration = Mockito.mock(Configuration.class);
            Builder mockedBuilder = Mockito.mock(Builder.class);
            Tracer mockedTracer = Mockito.mock(JaegerTracer.class);

            mockedStaticConfiguration.when(() -> Configuration.fromEnv()).thenReturn(mockedInstanceConfiguration);
            mockedStaticConfiguration.when(() -> mockedInstanceConfiguration.withMetricsFactory(Mockito.any()))
                    .thenReturn(mockedInstanceConfiguration);
            mockedStaticConfiguration.when(() -> mockedInstanceConfiguration.getTracerBuilder())
                    .thenReturn(mockedBuilder);
            mockedStaticConfiguration.when(() -> mockedBuilder.withScopeManager(Mockito.any()))
                    .thenReturn(mockedBuilder);
            mockedStaticConfiguration.when(() -> mockedBuilder.withReporter(Mockito.any())).thenReturn(mockedBuilder);
            mockedStaticConfiguration.when(() -> mockedBuilder.build()).thenReturn(mockedTracer);

            QuarkusJaegerTracer tracer = new QuarkusJaegerTracer();
            tracer.setZipkinCompatibilityMode(true);
            tracer.setEndpoint("http://localhost");
            tracer.toString();
            tracer.close();

            ArgumentCaptor<Reporter> argument = ArgumentCaptor.forClass(Reporter.class);
            Mockito.verify(mockedBuilder).withReporter(argument.capture());
            assertEquals(ZipkinV2Reporter.class, argument.getValue().getClass());
        }

    }

    @Test
    public void withoutZipkinCompatibilityMode() {
        try (MockedStatic<Configuration> mockedStaticConfiguration = Mockito.mockStatic(Configuration.class)) {
            Configuration mockedInstanceConfiguration = Mockito.mock(Configuration.class);
            Builder mockedBuilder = Mockito.mock(Builder.class);
            Tracer mockedTracer = Mockito.mock(JaegerTracer.class);

            mockedStaticConfiguration.when(() -> Configuration.fromEnv()).thenReturn(mockedInstanceConfiguration);
            mockedStaticConfiguration.when(() -> mockedInstanceConfiguration.withMetricsFactory(Mockito.any()))
                    .thenReturn(mockedInstanceConfiguration);
            mockedStaticConfiguration.when(() -> mockedInstanceConfiguration.getTracerBuilder())
                    .thenReturn(mockedBuilder);
            mockedStaticConfiguration.when(() -> mockedBuilder.withScopeManager(Mockito.any()))
                    .thenReturn(mockedBuilder);
            mockedStaticConfiguration.when(() -> mockedBuilder.withReporter(Mockito.any())).thenReturn(mockedBuilder);
            mockedStaticConfiguration.when(() -> mockedBuilder.build()).thenReturn(mockedTracer);

            QuarkusJaegerTracer tracer = new QuarkusJaegerTracer();
            tracer.toString();
            tracer.close();

            ArgumentCaptor<Reporter> argument = ArgumentCaptor.forClass(Reporter.class);
            Mockito.verify(mockedBuilder).withReporter(argument.capture());
            assertNull(argument.getValue());
        }
    }

}
