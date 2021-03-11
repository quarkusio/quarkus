package io.quarkus.resteasy.reactive.client.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;

/**
 * Alter jaxrs client proxy generation
 */
public interface JaxrsClientEnricher {
    /**
     * Class-level alterations
     * 
     * @param ctor jaxrs client constructor
     * @param globalTarget WebTarget field of the jaxrs client
     * @param interfaceClass JAXRS-annotated interface for which the client is being generated
     * @param index jandex index
     */
    void forClass(MethodCreator ctor, AssignableResultHandle globalTarget,
            ClassInfo interfaceClass, IndexView index);

    /**
     * Method-level alterations
     * 
     * @param classCreator creator of the jaxrs stub class
     * @param constructor constructor of the jaxrs stub class
     * @param methodCreator the method that is being generated, e.g. a method corresponding to `@GET Response get()`
     * @param interfaceClass JAXRS-annotated interface for which the client is being generated
     * @param method jandex method object corresponding to the method
     * @param methodWebTarget method-level WebTarget
     * @param index jandex index
     * @param generatedClasses build producer used to generate classes. Used e.g. to generate classes for header filling
     * @param methodIndex 0-based index of the method in the class. Used to assure there is no clash in generating classes
     * @return customizer for Invocation.Builder
     */
    void forMethod(ClassCreator classCreator, MethodCreator constructor, MethodCreator methodCreator,
            ClassInfo interfaceClass, MethodInfo method,
            AssignableResultHandle invocationBuilder, IndexView index,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            int methodIndex);
}
