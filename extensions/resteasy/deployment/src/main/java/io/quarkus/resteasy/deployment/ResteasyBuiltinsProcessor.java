package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.runtime.RolesFilterRegistrar;
import io.quarkus.resteasy.server.common.runtime.QuarkusInjectorFactory;

public class ResteasyBuiltinsProcessor {
    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setupFilter(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(RolesFilterRegistrar.class.getName()));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(RUNTIME_INIT)
    void setupExceptionMapper(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(NotFoundExceptionMapper.class.getName()));
    }

    //@BuildStep
    void addCommonReflectionClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                ResteasyProviderFactoryImpl.class,
                QuarkusInjectorFactory.class));
        // StringParameterInjector todo fix resteasy so that it doesn't need to reflect on String.class!
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                String.class));
    }

}
