package io.quarkus.jaxrs.client.reactive.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;

/**
 * Alter jaxrs client proxy generation
 */
public interface JaxrsClientReactiveEnricher {
    /**
     * Class-level alterations
     *
     * Used by MicroProfile Rest Client implementation (quarkus-rest-client-reactive) to support
     * {@link jakarta.ws.rs.ext.Provider}, {@code @ClientHeadersFactory}, etc
     *
     * Please note that this won't be invoked for sub-resources
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
     * @param invocationBuilder assignable reference for Invocation.Builder
     * @param index jandex index
     * @param generatedClasses build producer used to generate classes. Used e.g. to generate classes for header filling
     * @param methodIndex 0-based index of the method in the interface. Used to assure there is no clash in generating classes
     * @param javaMethodField method reference in a static class field
     */
    void forMethod(ClassCreator classCreator, MethodCreator constructor,
            MethodCreator clinit,
            MethodCreator methodCreator,
            ClassInfo interfaceClass,
            MethodInfo method, AssignableResultHandle invocationBuilder,
            IndexView index, BuildProducer<GeneratedClassBuildItem> generatedClasses, int methodIndex,
            FieldDescriptor javaMethodField);

    /**
     * Method-level alterations for methods of sub-resources
     *
     * @param subClassCreator creator of the sub-resource stub class
     * @param subConstructor constructor of the sub-resource stub class
     * @param subMethodCreator the method that is being generated
     * @param rootInterfaceClass root JAX-RS interface for which the client is being generated
     * @param subInterfaceClass sub-resource JAX-RS interface for which the client is being generated
     * @param subMethod jandex method object corresponding to the current sub-resource method
     * @param rootMethod jandex method object corresponding to the current root resource method
     * @param invocationBuilder Invocation.Builder's assignable reference. Local for subMethod
     * @param index jandex index
     * @param generatedClasses build producer used to generate classes
     * @param methodIndex 0-based index of method in the root interface
     * @param subMethodIndex index of the method in the sub-resource interface
     * @param javaMethodField method reference in a static class field
     */
    void forSubResourceMethod(ClassCreator subClassCreator, MethodCreator subConstructor,
            MethodCreator subClinit,
            MethodCreator subMethodCreator, ClassInfo rootInterfaceClass, ClassInfo subInterfaceClass,
            MethodInfo subMethod, MethodInfo rootMethod, AssignableResultHandle invocationBuilder, // sub-level
            IndexView index, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            int methodIndex, int subMethodIndex, FieldDescriptor javaMethodField);
}
