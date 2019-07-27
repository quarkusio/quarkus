package io.quarkus.resteasy.jackson.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.jackson.runtime.ObjectMapperProducer;

public class ResteasyJacksonProcessor {

    private static final DotName OBJECT_MAPPER = DotName.createSimple(ObjectMapper.class.getName());
    private static final DotName CONTEXT_RESOLVER = DotName.createSimple(ContextResolver.class.getName());

    private static final String QUARKUS_CONTEXT_RESOLVER_NAME = "io.quarkus.resteasy.jackson.runtime.QuarkusObjectMapperContextResolver";

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JACKSON));
    }

    @BuildStep
    void register(CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<UnremovableBeanBuildItem> unremovable) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer"));

        IndexView index = combinedIndexBuildItem.getIndex();

        // if the user has declared a custom ContextResolver for ObjectMapper, we don't generate anything
        if (hasCustomContextResolverBeenSupplied(index)) {
            return;
        }

        generateObjectMapperContextResolver(new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
            }
        });
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(QUARKUS_CONTEXT_RESOLVER_NAME));

        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(ObjectMapperProducer.class));
        Set<String> userSuppliedProducers = getUserSuppliedJacksonProducerBeans(index);
        if (!userSuppliedProducers.isEmpty()) {
            unremovable.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(userSuppliedProducers)));
        }
    }

    private boolean hasCustomContextResolverBeenSupplied(IndexView index) {
        for (ClassInfo contextResolver : index.getAllKnownImplementors(CONTEXT_RESOLVER)) {
            if (contextResolver.classAnnotation(DotName.createSimple(Provider.class.getName())) == null) {
                continue;
            }

            for (Type interfacesType : contextResolver.interfaceTypes()) {
                if (!CONTEXT_RESOLVER.equals(interfacesType.name())) {
                    continue;
                }

                // make sure we are only dealing with implementations that have set the generic type of ContextResolver
                if (!(interfacesType instanceof ParameterizedType)) {
                    continue;
                }

                List<Type> contextResolverGenericArguments = interfacesType.asParameterizedType().arguments();
                if (contextResolverGenericArguments.size() != 1) {
                    continue; // shouldn't ever happen
                }

                Type firstGenericType = contextResolverGenericArguments.get(0);
                if ((firstGenericType instanceof ClassType) &&
                        firstGenericType.asClassType().name().equals(OBJECT_MAPPER)) {
                    return true;
                }
            }
        }
        return false;
    }

    // we need to find all the user supplied producers and mark them as unremovable since there are no actual injection points
    // for the ObjectMapper
    private Set<String> getUserSuppliedJacksonProducerBeans(IndexView index) {
        Set<String> result = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple("javax.enterprise.inject.Produces"))) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (OBJECT_MAPPER.equals(annotation.target().asMethod().returnType().name())) {
                result.add(annotation.target().asMethod().declaringClass().name().toString());
            }
        }
        return result;
    }

    // we generate a javax.ws.rs.ext.ContextResolver for ObjectMapper that pulls the ObjectMapper out of Arc
    // thus ensuring that the configured ObjectMapper bean is used (whether it's the default bean or the user supplied bean).
    // The reason we need to generate this class instead of just including it at runtime is to ensure that
    // when a user goes all the way and supplies their ContextResolver for ObjectMapper, that we don't interfere
    // with that at all
    private void generateObjectMapperContextResolver(ClassOutput classOutput) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(QUARKUS_CONTEXT_RESOLVER_NAME)
                .interfaces(ContextResolver.class)
                .signature("Ljava/lang/Object;Ljavax/ws/rs/ext/ContextResolver<Lcom/fasterxml/jackson/databind/ObjectMapper;>;")
                .build()) {

            cc.addAnnotation(Provider.class);

            // cache the instance for faster lookup
            FieldDescriptor instance = cc.getFieldCreator("INSTANCE", ObjectMapper.class)
                    .setModifiers(Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator getContext = cc.getMethodCreator("getContext", ObjectMapper.class, Class.class)) {
                BranchResult branchResult = getContext.ifNull(getContext.readStaticField(instance));

                BytecodeCreator instanceNotNull = branchResult.falseBranch();
                instanceNotNull.returnValue(instanceNotNull.readStaticField(instance));

                BytecodeCreator instanceNull = branchResult.trueBranch();

                ResultHandle arcContainer = instanceNull
                        .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));

                ResultHandle jsonbClass = instanceNull.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class),
                        instanceNull.load(ObjectMapper.class.getName()));
                ResultHandle instanceHandle = instanceNull.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                Annotation[].class),
                        arcContainer, jsonbClass, instanceNull.loadNull());
                ResultHandle get = instanceNull.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                        instanceHandle);

                ResultHandle objectMapper = instanceNull.checkCast(get, ObjectMapper.class);

                instanceNull.writeStaticField(instance, objectMapper);
                instanceNull.returnValue(objectMapper);
            }

            try (MethodCreator bridgeGetContext = cc.getMethodCreator("getContext", Object.class, Class.class)) {
                MethodDescriptor getContext = MethodDescriptor.ofMethod(QUARKUS_CONTEXT_RESOLVER_NAME, "getContext",
                        "com.fasterxml.jackson.databind.ObjectMapper",
                        "java.lang.Class");
                ResultHandle result = bridgeGetContext.invokeVirtualMethod(getContext, bridgeGetContext.getThis(),
                        bridgeGetContext.getMethodParam(0));
                bridgeGetContext.returnValue(result);
                bridgeGetContext.returnValue(bridgeGetContext.readStaticField(instance));
            }
        }
    }
}
