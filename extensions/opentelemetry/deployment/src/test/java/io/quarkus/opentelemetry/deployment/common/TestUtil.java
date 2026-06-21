package io.quarkus.opentelemetry.deployment.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.Unremovable;

@Unremovable
public final class TestUtil {
    private TestUtil() {
    }

    public static Object getSharedState(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        TracerProvider tracerProvider = openTelemetry.getTracerProvider();
        Method unobfuscateMethod = tracerProvider.getClass().getDeclaredMethod("unobfuscate");
        unobfuscateMethod.setAccessible(true);
        SdkTracerProvider sdkTracerProvider = (SdkTracerProvider) unobfuscateMethod.invoke(tracerProvider);
        Field privateSharedStateField = sdkTracerProvider.getClass().getDeclaredField("sharedState");
        privateSharedStateField.setAccessible(true);
        return privateSharedStateField.get(sdkTracerProvider);
    }

    public static IdGenerator getIdGenerator(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object sharedState = getSharedState(openTelemetry);
        Field privateIdGeneratorField = sharedState.getClass().getDeclaredField("idGenerator");
        privateIdGeneratorField.setAccessible(true);
        return (IdGenerator) privateIdGeneratorField.get(sharedState);
    }

    public static Resource getResource(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object sharedState = getSharedState(openTelemetry);
        Field privateResourceField = sharedState.getClass().getDeclaredField("resource");
        privateResourceField.setAccessible(true);
        return (Resource) privateResourceField.get(sharedState);
    }

    public static Sampler getSampler(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object sharedState = getSharedState(openTelemetry);
        Field privateSamplerField = sharedState.getClass().getDeclaredField("sampler");
        privateSamplerField.setAccessible(true);
        return (Sampler) privateSamplerField.get(sharedState);
    }

    public static TextMapPropagator[] getTextMapPropagators(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException {
        TextMapPropagator textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
        Field privatePropagatorsField = textMapPropagator.getClass().getDeclaredField("textMapPropagators");
        privatePropagatorsField.setAccessible(true);
        return (TextMapPropagator[]) privatePropagatorsField.get(textMapPropagator);
    }

    public static void assertStringAttribute(SpanData spanData, AttributeKey<String> attributeKey, String expectedValue) {
        assertEquals(expectedValue, spanData.getAttributes().get(attributeKey), "Attribute Key Named:" + attributeKey.getKey());
    }

    public static SpanProcessor getActiveSpanProcessor(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object sharedState = getSharedState(openTelemetry);
        Field activeSpanProcessorField = sharedState.getClass().getDeclaredField("activeSpanProcessor");
        activeSpanProcessorField.setAccessible(true);
        return (SpanProcessor) activeSpanProcessorField.get(sharedState);
    }

    @SuppressWarnings("unchecked")
    public static BatchSpanProcessor getBatchSpanProcessor(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        SpanProcessor activeProcessor = getActiveSpanProcessor(openTelemetry);

        if (activeProcessor instanceof BatchSpanProcessor) {
            return (BatchSpanProcessor) activeProcessor;
        }

        try {
            Field spanProcessorsAllField = activeProcessor.getClass().getDeclaredField("spanProcessorsAll");
            spanProcessorsAllField.setAccessible(true);
            List<SpanProcessor> processors = (List<SpanProcessor>) spanProcessorsAllField.get(activeProcessor);
            for (SpanProcessor processor : processors) {
                if (processor instanceof BatchSpanProcessor) {
                    return (BatchSpanProcessor) processor;
                }
            }
        } catch (NoSuchFieldException e) {
            // Not a MultiSpanProcessor, ignore
        }

        throw new IllegalStateException("No BatchSpanProcessor found in the active span processor: "
                + activeProcessor.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    public static List<SpanExporter> getAllSpanExporters(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        SpanProcessor activeProcessor = getActiveSpanProcessor(openTelemetry);
        List<SpanExporter> exporters = new java.util.ArrayList<>();

        collectExportersFromProcessor(activeProcessor, exporters);

        if (exporters.isEmpty()) {
            try {
                Field spanProcessorsAllField = activeProcessor.getClass().getDeclaredField("spanProcessorsAll");
                spanProcessorsAllField.setAccessible(true);
                List<SpanProcessor> processors = (List<SpanProcessor>) spanProcessorsAllField.get(activeProcessor);
                for (SpanProcessor processor : processors) {
                    collectExportersFromProcessor(processor, exporters);
                }
            } catch (NoSuchFieldException e) {
                // Not a MultiSpanProcessor
            }
        }

        return exporters;
    }

    @SuppressWarnings("unchecked")
    public static List<SpanProcessor> getSpanProcessors(OpenTelemetry openTelemetry)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        SpanProcessor activeProcessor = getActiveSpanProcessor(openTelemetry);

        try {
            Field spanProcessorsAllField = activeProcessor.getClass().getDeclaredField("spanProcessorsAll");
            spanProcessorsAllField.setAccessible(true);
            return (List<SpanProcessor>) spanProcessorsAllField.get(activeProcessor);
        } catch (NoSuchFieldException e) {
            return List.of(activeProcessor);
        }
    }

    private static void collectExportersFromProcessor(SpanProcessor processor, List<SpanExporter> target)
            throws NoSuchFieldException, IllegalAccessException {
        if (processor instanceof BatchSpanProcessor bsp) {
            collectExporters(bsp.getSpanExporter(), target);
        } else if (processor instanceof SimpleSpanProcessor ssp) {
            collectExporters(ssp.getSpanExporter(), target);
        }
    }

    private static void collectExporters(SpanExporter exporter, List<SpanExporter> target)
            throws NoSuchFieldException, IllegalAccessException {
        if ("MultiSpanExporter".equals(exporter.getClass().getSimpleName())) {
            Field exportersField = exporter.getClass().getDeclaredField("spanExporters");
            exportersField.setAccessible(true);
            SpanExporter[] inner = (SpanExporter[]) exportersField.get(exporter);
            Collections.addAll(target, inner);
        } else {
            target.add(exporter);
        }
    }

    private static Object getBatchSpanProcessorWorker(BatchSpanProcessor bsp)
            throws NoSuchFieldException, IllegalAccessException {
        Field workerField = BatchSpanProcessor.class.getDeclaredField("worker");
        workerField.setAccessible(true);
        return workerField.get(bsp);
    }

    public static long getBspScheduleDelayNanos(BatchSpanProcessor bsp)
            throws NoSuchFieldException, IllegalAccessException {
        Object worker = getBatchSpanProcessorWorker(bsp);
        Field field = worker.getClass().getDeclaredField("scheduleDelayNanos");
        field.setAccessible(true);
        return (long) field.get(worker);
    }

    public static int getBspMaxExportBatchSize(BatchSpanProcessor bsp)
            throws NoSuchFieldException, IllegalAccessException {
        Object worker = getBatchSpanProcessorWorker(bsp);
        Field field = worker.getClass().getDeclaredField("maxExportBatchSize");
        field.setAccessible(true);
        return (int) field.get(worker);
    }

    public static long getBspExporterTimeoutNanos(BatchSpanProcessor bsp)
            throws NoSuchFieldException, IllegalAccessException {
        Object worker = getBatchSpanProcessorWorker(bsp);
        Field field = worker.getClass().getDeclaredField("exporterTimeoutNanos");
        field.setAccessible(true);
        return (long) field.get(worker);
    }

    public static int getBspMaxQueueSize(BatchSpanProcessor bsp)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        Object worker = getBatchSpanProcessorWorker(bsp);
        Field queueField = worker.getClass().getDeclaredField("queue");
        queueField.setAccessible(true);
        Object queue = queueField.get(worker);
        // The queue implements MessagePassingQueue which has a capacity() method
        Method capacityMethod = queue.getClass().getMethod("capacity");
        capacityMethod.setAccessible(true);
        return (int) capacityMethod.invoke(queue);
    }

    public static SpanProcessor getSimpleSpanProcessorDelegate(SpanProcessor wrapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field delegateField = wrapper.getClass().getDeclaredField("delegate");
        delegateField.setAccessible(true);
        return (SpanProcessor) delegateField.get(wrapper);
    }

    public static SpanProcessor getReplacedBatchProcessor(SpanProcessor wrapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field replacedField = wrapper.getClass().getDeclaredField("replacedBatchProcessor");
        replacedField.setAccessible(true);
        return (SpanProcessor) replacedField.get(wrapper);
    }

    public static boolean isShutdown(SimpleSpanProcessor processor)
            throws NoSuchFieldException, IllegalAccessException {
        Field isShutdownField = SimpleSpanProcessor.class.getDeclaredField("isShutdown");
        isShutdownField.setAccessible(true);
        return ((AtomicBoolean) isShutdownField.get(processor)).get();
    }

    public static boolean isShutdown(BatchSpanProcessor processor)
            throws NoSuchFieldException, IllegalAccessException {
        Field isShutdownField = BatchSpanProcessor.class.getDeclaredField("isShutdown");
        isShutdownField.setAccessible(true);
        return ((AtomicBoolean) isShutdownField.get(processor)).get();
    }
}
