package io.quarkus.hibernate.accessor.deployment;

import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.TypeMetadata;
import static io.quarkus.hibernate.accessor.deployment.HibernateAccessorGenerationUtil.fqcnToName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.Builder;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.ConstructorMetadata;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.FieldMetadata;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.MemberMetadata;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem.MethodMetadata;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorBuildTimeConfig;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorRecorder;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorStrategy;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

class HibernateAccessorProcessor {

    private static final DotName REFLECTION_FREE_ACCESSOR = DotName.createSimple(ReflectionFreeAccessor.class);

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(Feature.HIBERNATE_ACCESSOR));
    }

    @BuildStep
    void findExtraTypesToProcess(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<HibernateAccessorBuildItem> accessorBuildItemBuildProducer) {
        Map<String, Builder> builders = new HashMap<>();

        IndexView index = combinedIndexBuildItem.getIndex();
        for (AnnotationInstance annotation : index.getAnnotations(REFLECTION_FREE_ACCESSOR)) {

            final AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case CLASS -> builders.computeIfAbsent(target.asClass().name().toString(),
                        modelClass -> new Builder(target.asClass()).all(target.asClass()));
                case FIELD -> builders.computeIfAbsent(target.asField().declaringClass().name().toString(),
                        modelClass -> new Builder(index.getClassByName(modelClass)))
                        .addField(target.asField());
                case METHOD -> {
                    MethodInfo method = target.asMethod();
                    Builder builder = builders.computeIfAbsent(
                            method.declaringClass().name().toString(),
                            modelClass -> new Builder(index.getClassByName(modelClass)));
                    if (method.isConstructor()) {
                        builder.addConstructor(method);
                    } else if (method.parametersCount() == 0) {
                        builder.addGetter(method);
                    } else if (method.parametersCount() == 1) {
                        builder.addSetter(method);
                    } else {
                        throw new UnsupportedOperationException(
                                "Methods with more than one parameter cannot be getters/setters. Method " + method
                                        + " cannot be processed.");
                    }
                }
                default -> throw new UnsupportedOperationException(
                        "Only fields, getters and setters can be annotated with " + REFLECTION_FREE_ACCESSOR);
            }
        }
        for (Builder builder : builders.values()) {
            accessorBuildItemBuildProducer.produce(builder.build());
        }

    }

    @BuildStep
    void generateDirectAccessors(
            HibernateAccessorBuildTimeConfig config,
            List<HibernateAccessorBuildItem> hibernateAccessorBuildItemList,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformer) {
        if (config.strategy() == HibernateAccessorStrategy.REFLECTION) {
            return;
        }

        List<ProcessedHostData> hosts = new ArrayList<>();
        ProcessedHostData currentType = null;

        for (HibernateAccessorBuildItem accessItem : hibernateAccessorBuildItemList) {
            if (currentType == null || !accessItem.getType().name().equals(currentType.type().name())) {
                currentType = new ProcessedHostData(accessItem.getType());
                hosts.add(currentType);
            }

            for (FieldMetadata field : accessItem.getFields()) {
                currentType.readers.add(field);

                if (!field.readOnly()) {
                    currentType.writers.add(field);
                }
            }

            currentType.readers.addAll(accessItem.getGetters());
            currentType.writers.addAll(accessItem.getSetters());
            currentType.constructors.addAll(accessItem.getConstructors());
        }

        HibernateAccessorFactoryImplementation factoryImpl = new HibernateAccessorFactoryImplementation();
        HibernateAccessorBridgeGenerator bridgeGen = new HibernateAccessorBridgeGenerator();

        int currentClassIndex = 0;
        for (ProcessedHostData data : hosts) {
            // See TypeMetadata.needsBridge() for why non-public hosts are dispatched to via a bridge.
            boolean needsBridge = data.type().needsBridge();
            String dispatchTargetInternal = fqcnToName(data.type().dispatchTarget());

            // Always register the host for all accessor types
            factoryImpl.registerDispatchTarget(data.type().host(), dispatchTargetInternal,
                    data.type().dispatchTargetIsInterface());
            factoryImpl.registerFieldReader(data.type().host());
            factoryImpl.registerMethodReader(data.type().host());
            factoryImpl.registerFieldWriter(data.type().host());
            factoryImpl.registerMethodWriter(data.type().host());
            factoryImpl.registerInstantiator(data.type().host());

            if (needsBridge) {
                generatedClasses.produce(new GeneratedClassBuildItem(true,
                        HibernateAccessorBridgeGenerator.bridgeFqcn(data.type().host()),
                        bridgeGen.generate(data.type().host(), data.type().isInterface())));
            }

            transformer.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(data.type().host())
                    .setCacheable(true)
                    // We need to make sure that we run this transformation
                    // *after* Hibernate ORM and Panache are done with their changes:
                    .setPriority(HibernateAccessorHostClassFunction.ACCESSOR_TRANSFORMATION_PRIORITY)
                    .setVisitorFunction(new HibernateAccessorHostClassFunction(
                            data.readers, data.writers, data.constructors, currentClassIndex++))
                    .build());
        }

        HibernateAccessorSingleImplGenerator implGen = new HibernateAccessorSingleImplGenerator();

        generatedClasses.produce(new GeneratedClassBuildItem(true,
                HibernateAccessorGeneratorConstants.GENERATED_READER_IMPL,
                implGen.generateReaderImpl(hosts)));
        generatedClasses.produce(new GeneratedClassBuildItem(true,
                HibernateAccessorGeneratorConstants.GENERATED_WRITER_IMPL,
                implGen.generateWriterImpl(hosts)));
        generatedClasses.produce(new GeneratedClassBuildItem(true,
                HibernateAccessorGeneratorConstants.GENERATED_INSTANTIATOR_IMPL,
                implGen.generateInstantiatorImpl(hosts)));

        boolean withFallback = config.strategy() == HibernateAccessorStrategy.REFLECTION_FREE_WITH_FALLBACK;
        generatedClasses.produce(new GeneratedClassBuildItem(true,
                HibernateAccessorGeneratorConstants.QUARKUS_HIBERNATE_ACCESSOR_FACTORY,
                factoryImpl.generate(withFallback)));
    }

    @BuildStep
    void registerForReflection(
            HibernateAccessorBuildTimeConfig config,
            List<HibernateAccessorBuildItem> accessorBuildItems,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (config.strategy() == HibernateAccessorStrategy.REFLECTION_FREE) {
            reflectiveClasses.produce(ReflectiveClassBuildItem
                    .builder(HibernateAccessorGeneratorConstants.QUARKUS_HIBERNATE_ACCESSOR_FACTORY).constructors().build());
        } else {
            String reason = getClass().getName();
            for (HibernateAccessorBuildItem item : accessorBuildItems) {
                for (FieldMetadata field : item.getFields()) {
                    reflectiveFields.produce(new ReflectiveFieldBuildItem(reason, field.declaringClass(), field.name()));
                }
                for (MethodMetadata getter : item.getGetters()) {
                    reflectiveMethods.produce(
                            new ReflectiveMethodBuildItem(reason, getter.declaringClass(), getter.name(), new String[0]));
                }
                for (MethodMetadata setter : item.getSetters()) {
                    reflectiveMethods.produce(
                            new ReflectiveMethodBuildItem(reason, setter.declaringClass(), setter.name(),
                                    setter.parameterTypes()));
                }
                for (ConstructorMetadata ctor : item.getConstructors()) {
                    reflectiveClasses.produce(
                            ReflectiveClassBuildItem.builder(ctor.declaringClass()).constructors().build());
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    HibernateAccessorFactoryBuildItem accessFActory(
            HibernateAccessorBuildTimeConfig config,
            HibernateAccessorRecorder recorder) {
        switch (config.strategy()) {
            case REFLECTION -> {
                return new HibernateAccessorFactoryBuildItem(recorder.createReflectionFactory());
            }
            case REFLECTION_FREE_WITH_FALLBACK -> {
                recorder.initAccessorImplFactory(
                        HibernateAccessorGeneratorConstants.GENERATED_READER_IMPL,
                        HibernateAccessorGeneratorConstants.GENERATED_WRITER_IMPL,
                        HibernateAccessorGeneratorConstants.GENERATED_INSTANTIATOR_IMPL);
                return new HibernateAccessorFactoryBuildItem(
                        recorder.createAccessorFactoryWithFallback(
                                HibernateAccessorFactoryImplementation.QUARKUS_HIBERNATE_ACCESSOR_FACTORY));
            }
            default -> {
                recorder.initAccessorImplFactory(
                        HibernateAccessorGeneratorConstants.GENERATED_READER_IMPL,
                        HibernateAccessorGeneratorConstants.GENERATED_WRITER_IMPL,
                        HibernateAccessorGeneratorConstants.GENERATED_INSTANTIATOR_IMPL);
                return new HibernateAccessorFactoryBuildItem(
                        recorder.createAccessorFactory(
                                HibernateAccessorGeneratorConstants.QUARKUS_HIBERNATE_ACCESSOR_FACTORY));
            }
        }
    }

    record ProcessedHostData(TypeMetadata type,
            Set<MemberMetadata> readers,
            Set<MemberMetadata> writers,
            Set<ConstructorMetadata> constructors) {

        private ProcessedHostData(TypeMetadata type) {
            this(type, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
        }
    }
}
