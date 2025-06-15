package org.jboss.resteasy.reactive.server.processor.generation.injection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.generation.AbstractFeatureScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.InjectedClassConverterField;

public class FieldInjectionFeature extends AbstractFeatureScanner {

    final List<InjectedClassConverterField> initMethods = new ArrayList<>();

    @Override
    public void integrateWithIndexer(ServerEndpointIndexer.Builder builder, IndexView index) {
        builder.setFieldInjectionIndexerExtension(
                new TransformedFieldInjectionIndexerExtension(transformations::put, true, initMethods::add));

    }

    public void runtimeInit(ClassLoader classLoader, Deployment deployment) {
        for (InjectedClassConverterField i : initMethods) {
            try {
                Class<?> theClass = Class.forName(i.getInjectedClassName().replace("/", "."), false, classLoader);
                Method theMethod = theClass.getDeclaredMethod(i.getMethodName(), Deployment.class);
                theMethod.invoke(null, deployment);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to initialize converters for injected class" + i.getInjectedClassName(), e);
            }
        }
    }
}
