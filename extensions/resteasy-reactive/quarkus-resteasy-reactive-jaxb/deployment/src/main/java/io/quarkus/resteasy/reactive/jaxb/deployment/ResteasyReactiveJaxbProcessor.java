package io.quarkus.resteasy.reactive.jaxb.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jaxb.deployment.JaxbClassesToBeBoundBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.JaxRsResourceIndexBuildItem;
import io.quarkus.resteasy.reactive.jaxb.runtime.serialisers.ServerJaxbMessageBodyReader;
import io.quarkus.resteasy.reactive.jaxb.runtime.serialisers.ServerJaxbMessageBodyWriter;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveResourceMethodEntriesBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJaxbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_JAXB));
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ServerJaxbMessageBodyReader.class.getName())
                .addBeanClass(ServerJaxbMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(ServerJaxbMessageBodyReader.class.getName(), Object.class.getName(),
                        List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML), RuntimeType.SERVER, true, Priorities.USER));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(ServerJaxbMessageBodyWriter.class.getName(), Object.class.getName(),
                        List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML), RuntimeType.SERVER, true, Priorities.USER));
    }

    @BuildStep
    void registerClassesToBeBound(ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntries,
            JaxRsResourceIndexBuildItem index,
            BuildProducer<JaxbClassesToBeBoundBuildItem> classesToBeBoundBuildItemProducer) {
        Set<ClassInfo> classesInfo = new HashSet<>();

        IndexView indexView = index.getIndexView();
        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : resourceMethodEntries.getEntries()) {
            ResourceMethod resourceInfo = entry.getResourceMethod();
            MethodInfo methodInfo = entry.getMethodInfo();
            ClassInfo effectiveReturnType = getEffectiveClassInfo(methodInfo.returnType(), indexView);

            if (effectiveReturnType != null) {
                // When using "application/xml", the return type needs to be registered
                if (producesXml(resourceInfo)) {
                    classesInfo.add(effectiveReturnType);
                }

                // When using "multipart/form-data", the parts that use "application/xml" need to be registered
                if (producesMultipart(resourceInfo)) {
                    classesInfo.addAll(getEffectivePartsUsingXml(effectiveReturnType, indexView));
                }
            }

            // If consumes "application/xml" or "multipart/form-data", we register all the classes of the parameters
            if (consumesXml(resourceInfo) || consumesMultipart(resourceInfo)) {
                for (Type parameter : methodInfo.parameterTypes()) {
                    ClassInfo effectiveParameter = getEffectiveClassInfo(parameter, indexView);
                    if (effectiveParameter != null) {
                        classesInfo.add(effectiveParameter);
                    }
                }
            }
        }

        classesToBeBoundBuildItemProducer.produce(new JaxbClassesToBeBoundBuildItem(toClasses(classesInfo)));
    }

    @BuildStep
    void setupJaxbContextConfigForValidator(Capabilities capabilities,
            BuildProducer<JaxbClassesToBeBoundBuildItem> classesToBeBoundProducer) {
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            classesToBeBoundProducer.produce(new JaxbClassesToBeBoundBuildItem(
                    Collections.singletonList("io.quarkus.hibernate.validator.runtime.jaxrs.ViolationReport")));
        }
    }

    private List<ClassInfo> getEffectivePartsUsingXml(ClassInfo returnType, IndexView indexView) {
        List<ClassInfo> classInfos = new ArrayList<>();
        for (FieldInfo field : returnType.fields()) {
            AnnotationInstance partTypeInstance = field.annotation(ResteasyReactiveDotNames.PART_TYPE_NAME);
            if (partTypeInstance != null) {
                AnnotationValue partTypeValue = partTypeInstance.value();
                if (partTypeValue != null && MediaType.APPLICATION_XML.equals(partTypeValue.asString())) {
                    classInfos.add(getEffectiveClassInfo(field.type(), indexView));
                }
            }
        }

        return classInfos;
    }

    private ClassInfo getEffectiveClassInfo(Type type, IndexView indexView) {
        if (type.kind() == Type.Kind.VOID || type.kind() == Type.Kind.PRIMITIVE) {
            return null;
        }

        Type effectiveType = type;
        if (effectiveType.name().equals(ResteasyReactiveDotNames.REST_RESPONSE) ||
                effectiveType.name().equals(ResteasyReactiveDotNames.UNI) ||
                effectiveType.name().equals(ResteasyReactiveDotNames.COMPLETABLE_FUTURE) ||
                effectiveType.name().equals(ResteasyReactiveDotNames.COMPLETION_STAGE) ||
                effectiveType.name().equals(ResteasyReactiveDotNames.MULTI)) {
            if (effectiveType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                return null;
            }

            effectiveType = type.asParameterizedType().arguments().get(0);
        }
        if (effectiveType.name().equals(ResteasyReactiveDotNames.SET) ||
                effectiveType.name().equals(ResteasyReactiveDotNames.COLLECTION) ||
                effectiveType.name().equals(ResteasyReactiveDotNames.LIST)) {
            effectiveType = effectiveType.asParameterizedType().arguments().get(0);
        } else if (effectiveType.name().equals(ResteasyReactiveDotNames.MAP)) {
            effectiveType = effectiveType.asParameterizedType().arguments().get(1);
        }

        ClassInfo effectiveReturnClassInfo = indexView.getClassByName(effectiveType.name());
        if ((effectiveReturnClassInfo == null) || effectiveReturnClassInfo.name().equals(ResteasyReactiveDotNames.OBJECT)) {
            return null;
        }

        return effectiveReturnClassInfo;
    }

    private boolean consumesXml(ResourceMethod resourceInfo) {
        return containsMediaType(resourceInfo.getConsumes(), MediaType.APPLICATION_XML);
    }

    private boolean consumesMultipart(ResourceMethod resourceInfo) {
        return containsMediaType(resourceInfo.getConsumes(), MediaType.MULTIPART_FORM_DATA);
    }

    private boolean producesXml(ResourceMethod resourceInfo) {
        return containsMediaType(resourceInfo.getProduces(), MediaType.APPLICATION_XML);
    }

    private boolean producesMultipart(ResourceMethod resourceInfo) {
        return containsMediaType(resourceInfo.getProduces(), MediaType.MULTIPART_FORM_DATA);
    }

    private boolean containsMediaType(String[] types, String mediaType) {
        if (types != null) {
            for (String type : types) {
                if (type.toLowerCase(Locale.ROOT).contains(mediaType)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<String> toClasses(Collection<ClassInfo> classesInfo) {
        List<String> classes = new ArrayList<>();
        for (ClassInfo classInfo : classesInfo) {
            classes.add(classInfo.toString());
        }

        return classes;
    }
}
