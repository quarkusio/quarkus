package io.quarkus.infinispan.embedded.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.distribution.ch.ConsistentHashFactory;
import org.infinispan.distribution.ch.impl.HashFunctionPartitioner;
import org.infinispan.factories.impl.ModuleMetadataBuilder;
import org.infinispan.interceptors.AsyncInterceptor;
import org.infinispan.marshall.exts.CollectionExternalizer;
import org.infinispan.marshall.exts.MapExternalizer;
import org.infinispan.notifications.Listener;
import org.infinispan.persistence.spi.CacheLoader;
import org.infinispan.persistence.spi.CacheWriter;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jgroups.conf.PropertyConverter;
import org.jgroups.stack.Protocol;
import org.jgroups.util.Util;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.infinispan.embedded.runtime.InfinispanEmbeddedProducer;

class InfinispanEmbeddedProcessor {
    @BuildStep
    void addInfinispanDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.jgroups", "jgroups"));
        indexDependency.produce(new IndexDependencyBuildItem("org.infinispan", "infinispan-commons"));
        indexDependency.produce(new IndexDependencyBuildItem("org.infinispan", "infinispan-core"));
    }

    @BuildStep
    void setup(BuildProducer<FeatureBuildItem> feature, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServiceProviderBuildItem> serviceProvider, BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<NativeImageResourceBuildItem> resources, CombinedIndexBuildItem combinedIndexBuildItem,
            List<InfinispanReflectionExcludedBuildItem> excludedReflectionClasses,
            ApplicationIndexBuildItem applicationIndexBuildItem) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.INFINISPAN_EMBEDDED));

        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(InfinispanEmbeddedProducer.class));

        for (Class<?> serviceLoadedInterface : Arrays.asList(ModuleMetadataBuilder.class, ConfigurationParser.class)) {
            // Need to register all the modules as service providers so they can be picked up at runtime
            ServiceLoader<?> serviceLoader = ServiceLoader.load(serviceLoadedInterface);
            List<String> interfaceImplementations = new ArrayList<>();
            serviceLoader.forEach(mmb -> interfaceImplementations.add(mmb.getClass().getName()));
            if (!interfaceImplementations.isEmpty()) {
                serviceProvider
                        .produce(new ServiceProviderBuildItem(serviceLoadedInterface.getName(), interfaceImplementations));
            }
        }

        // These are either default or required for marshalling
        resources.produce(new NativeImageResourceBuildItem(
                "org/infinispan/protostream/message-wrapping.proto",
                "proto/generated/persistence.commons.proto",
                "proto/generated/persistence.core.proto",
                "proto/generated/global.commons.proto",
                "default-configs/default-jgroups-udp.xml",
                "default-configs/default-jgroups-tcp.xml",
                "default-configs/default-jgroups-kubernetes.xml",
                "default-configs/default-jgroups-ec2.xml",
                "default-configs/default-jgroups-google.xml",
                "default-configs/default-jgroups-azure.xml"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, HashFunctionPartitioner.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JGroupsTransport.class));

        // XML reflection classes
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                "com.sun.xml.bind.v2.ContextFactory",
                "com.sun.xml.internal.bind.v2.ContextFactory",
                "com.sun.xml.internal.stream.XMLInputFactoryImpl"));

        CollectionExternalizer.getSupportedPrivateClasses()
                .forEach(ceClass -> reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ceClass)));
        MapExternalizer.getSupportedPrivateClasses()
                .forEach(ceClass -> reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ceClass)));

        Set<DotName> excludedClasses = new HashSet<>();
        excludedReflectionClasses.forEach(excludedBuildItem -> excludedClasses.add(excludedBuildItem.getExcludedClass()));

        IndexView combinedIndex = combinedIndexBuildItem.getIndex();

        // Add all the JGroups Protocols
        addReflectionForName(Protocol.class.getName(), false, combinedIndex, reflectiveClass, false, true, excludedClasses);

        // Add all the JGroups Property Converters
        addReflectionForClass(PropertyConverter.class, true, combinedIndex, reflectiveClass, excludedClasses);

        // Add all consistent hash factories
        addReflectionForClass(ConsistentHashFactory.class, true, combinedIndex, reflectiveClass, excludedClasses);

        // We have to add reflection for our own loaders and stores as well due to how configuration works
        addReflectionForClass(CacheLoader.class, true, combinedIndex, reflectiveClass, excludedClasses);
        addReflectionForClass(CacheWriter.class, true, combinedIndex, reflectiveClass, excludedClasses);

        // We have to include all of our interceptors - technically a custom one is installed before or after ISPN ones
        // If we don't want to support custom interceptors this should be removable
        addReflectionForClass(AsyncInterceptor.class, true, combinedIndex, reflectiveClass, excludedClasses);

        // We use our configuration builders for all of our supported loaders - this also handles user custom configuration
        // builders
        addReflectionForClass(StoreConfigurationBuilder.class, true, combinedIndex, reflectiveClass, excludedClasses);

        // We use reflection to load up the attributes for a store configuration
        addReflectionForName(StoreConfiguration.class.getName(), true, combinedIndex, reflectiveClass, true, false,
                excludedClasses);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, Util.AddressScope.class));

        // Add Infinispan and user listeners to reflection list
        Collection<AnnotationInstance> listenerInstances = combinedIndex.getAnnotations(
                DotName.createSimple(Listener.class.getName()));
        for (AnnotationInstance instance : listenerInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                DotName targetName = target.asClass().name();
                if (!excludedClasses.contains(targetName)) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, targetName.toString()));
                }
            }
        }

        // Infinispan has quite a few classes annotated with SerializeWith and user can use this - in the future
        // it would be nice to not have this required for Infinispan classes
        Collection<AnnotationInstance> serializeWith = combinedIndex
                .getAnnotations(DotName.createSimple(SerializeWith.class.getName()));
        for (AnnotationInstance instance : serializeWith) {
            AnnotationValue withValue = instance.value();
            String withValueString = withValue.asString();
            DotName targetSerializer = DotName.createSimple(withValueString);
            if (!excludedClasses.contains(targetSerializer)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, withValueString));
            }
        }

        // This contains parts from the index from the app itself
        Index appOnlyIndex = applicationIndexBuildItem.getIndex();

        // We only register the app advanced externalizers as all of the Infinispan ones are explicitly defined
        addReflectionForClass(AdvancedExternalizer.class, true, appOnlyIndex, reflectiveClass, Collections.emptySet());
        // Due to the index not containing AbstractExternalizer it doesn't know that it implements AdvancedExternalizer
        // thus we also have to include classes that extend AbstractExternalizer
        addReflectionForClass(AbstractExternalizer.class, false, appOnlyIndex, reflectiveClass, Collections.emptySet());
    }

    private void addReflectionForClass(Class<?> classToUse, boolean isInterface, IndexView indexView,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass, Set<DotName> excludedClasses) {
        addReflectionForName(classToUse.getName(), isInterface, indexView, reflectiveClass, false, false,
                excludedClasses);
    }

    private void addReflectionForName(String className, boolean isInterface, IndexView indexView,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass, boolean methods, boolean fields,
            Set<DotName> excludedClasses) {
        Collection<ClassInfo> classInfos;
        if (isInterface) {
            classInfos = indexView.getAllKnownImplementors(DotName.createSimple(className));
        } else {
            classInfos = indexView.getAllKnownSubclasses(DotName.createSimple(className));
        }

        classInfos.removeIf(ci -> excludedClasses.contains(ci.name()));

        if (!classInfos.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields,
                    classInfos.stream().map(ClassInfo::toString).toArray(String[]::new)));
        }
    }
}
