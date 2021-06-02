package io.quarkus.opentelemetry.deployment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
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
        System.out.println(textMapPropagator);
        Field privatePropagatorsField = textMapPropagator.getClass().getDeclaredField("textPropagators");
        privatePropagatorsField.setAccessible(true);
        return (TextMapPropagator[]) privatePropagatorsField.get(textMapPropagator);
    }
}
