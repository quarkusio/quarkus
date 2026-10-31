package io.quarkus.hibernate.accessor.deployment;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.hibernate.accessor.asm.aot.AotConstructorMetadata;
import org.hibernate.accessor.asm.aot.AotFieldMetadata;
import org.hibernate.accessor.asm.aot.AotMemberMetadata;
import org.hibernate.accessor.asm.aot.AotMethodMetadata;
import org.hibernate.accessor.asm.aot.AotTypeMetadata;
import org.hibernate.accessor.asm.aot.GenerationUtil;

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
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorBuildTimeConfig;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorRecorder;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorStrategy;

/**
 * Generates bytecode accessors using the Hibernate Accessor library's AOT emitters.
 * {@code generateDirectAccessors} registers {@link HibernateAccessorHostClassFunction} to inspect
 * enhanced hosts and emit their methods, and publishes the library-generated bridges, shared
 * accessor implementations and factory. This overview shows how those four pieces compose,
 * using one entity, {@code Model}, with a
 * {@code name} field and an {@code age} getter/setter, registered under host class index 0.
 *
 * <ol>
 * <li><b>Dispatch methods injected into the host class itself</b> (by {@link HibernateAccessorHostClassFunction}, via
 * a {@link BytecodeTransformerBuildItem}). These are added directly to {@code Model}'s own bytecode, so they can
 * access its private members directly:
 *
 * <pre>
 * // Injected into Model
 * public static Object $$__hibernateAccessor_read(int memberIndex, Object target) {
 *     switch (memberIndex) {
 *         case 0: return ((Model) target).name;
 *         case 1: return Integer.valueOf(((Model) target).getAge());
 *         default: throw new IllegalArgumentException(...);
 *     }
 * }
 * </pre>
 *
 * <li><b>An optional bridge class</b> (by {@link org.hibernate.accessor.asm.aot.AsmAotGenerator}), generated only when
 * {@code Model} is not public. Accessor implementations
 * always live in a fixed runtime package, which may differ from the host class's package, so they cannot call
 * the public dispatch methods on a non-public host class directly. The bridge is therefore generated as a public synthetic
 * class in the
 * host's package, forwarding calls to the host's injected methods. The class-level switch selects the host class;
 * the host's dispatch method then uses the member-level switch to access the requested member.
 * <p>
 * Public dispatch methods expose access to registered private members to callers that can access the host or bridge.
 * Generated accessors do not establish module reads or package exports, so access across named-module boundaries
 * can fail unless those permissions are already configured. Public visibility alone does not bypass these boundaries.
 * TODO: Define and test JPMS support for generated accessors, including placement of bridges in the host's module,
 * required module reads and package exports, and whether dispatch access should be restricted.
 *
 * <pre>
 * // Generated as Model$$HibernateAccessorBridge, in the same package as Model
 * public static Object $$__hibernateAccessor_read(int memberIndex, Object target) {
 *     return Model.$$__hibernateAccessor_read(memberIndex, target);
 * }
 * </pre>
 *
 * <li><b>One shared implementation class for each of the three accessor roles</b> &mdash; reader, writer and
 * instantiator (by {@link org.hibernate.accessor.asm.aot.AsmAotGenerator}) &mdash; generated once for the whole application
 * rather than once per member. Each instance just carries a host class index and a member index, and dispatches by
 * class index to whichever class was registered as that host's dispatch target in the previous step: {@code Model}
 * itself, or its bridge if one was generated:
 *
 * <pre>
 * // Generated ValueReader, shared by every registered class
 * public Object get(Object target) {
 *     switch (this.classIndex) {
 *         case 0: return Model.$$__hibernateAccessor_read(this.memberIndex, target);
 *         default: throw new IllegalArgumentException(...);
 *     }
 * }
 * </pre>
 *
 * The generated {@code ValueWriter} and {@code Instantiator} follow the same shape, calling the host's
 * {@code $$__hibernateAccessor_write}/{@code $$__hibernateAccessor_create} dispatch method instead:
 *
 * <pre>
 * // Generated ValueWriter
 * public void set(Object target, Object value) {
 *     switch (this.classIndex) {
 *         case 0: Model.$$__hibernateAccessor_write(this.memberIndex, target, value); return;
 *         default: throw new IllegalArgumentException(...);
 *     }
 * }
 *
 * // Generated Instantiator
 * public Object create(Object... arguments) {
 *     switch (this.classIndex) {
 *         case 0: return Model.$$__hibernateAccessor_create(this.memberIndex, arguments);
 *         default: throw new IllegalArgumentException(...);
 *     }
 * }
 * </pre>
 *
 * <li><b>A factory resolving member names to accessor instances</b> (by
 * {@link org.hibernate.accessor.asm.aot.AsmAotGenerator}), implementing the Hibernate {@code AccessorFactory} API.
 * It accepts reflective fields, methods or constructors, resolves the declaring class, and delegates to lookup methods
 * injected into the host (through its bridge when needed). Those lookup methods map a member key to an accessor instance
 * carrying the {@code (classIndex, memberIndex)} pair above. Field keys are names; method keys also include the descriptor,
 * and constructor keys are descriptors. A lookup returns {@code null} for an unknown key; the factory then throws or
 * delegates to reflection, depending on the configured strategy:
 *
 * <pre>
 * // Injected into Model, simplified; called by the generated factory
 * public static ValueReader $$__hibernateAccessor_fieldReader(String key) {
 *     switch (key) {
 *         case "name":
 *             return (ValueReader) AccessorImplFactory.createReader(0, 0);
 *         default:
 *             return null;
 *     }
 * }
 * </pre>
 * </ol>
 */
class HibernateAccessorProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(Feature.HIBERNATE_ACCESSOR));
    }

    @BuildStep
    void findExtraTypesToProcess(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<HibernateAccessorRegistrationBuildItem> accessorBuildItemBuildProducer) {
        HibernateAccessorAnnotationCollector.collect(combinedIndexBuildItem.getIndex())
                .forEach(accessorBuildItemBuildProducer::produce);
    }

    @BuildStep
    void generateDirectAccessors(
            HibernateAccessorBuildTimeConfig config,
            List<HibernateAccessorRegistrationBuildItem> hibernateAccessorBuildItemList,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformer) {
        if (config.strategy() == HibernateAccessorStrategy.REFLECTION) {
            return;
        }

        List<ProcessedHostData> hosts = mergeRegistrations(hibernateAccessorBuildItemList);
        int currentClassIndex = 0;
        for (ProcessedHostData data : hosts) {
            transformer.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(data.type().host())
                    // This transformation must not be cached across live reloads: the class index we
                    // bake into the host is its position in the sorted host list, and that position
                    // shifts as soon as another host is added or removed. Reusing the bytecode of an
                    // unchanged host would then dispatch to whichever host now sits at that position.
                    .setCacheable(false)
                    // We need to make sure that we run this transformation
                    // *after* Hibernate ORM and Panache are done with their changes:
                    .setPriority(HibernateAccessorHostClassFunction.ACCESSOR_TRANSFORMATION_PRIORITY)
                    .setVisitorFunction(new HibernateAccessorHostClassFunction(
                            data.type(), data.readers, data.writers, data.constructors, currentClassIndex++))
                    .build());
        }
        boolean withFallback = config.strategy() == HibernateAccessorStrategy.GENERATED_WITH_FALLBACK;
        HibernateAccessorGenerator.generator().generateClasses(
                hosts.stream().map(ProcessedHostData::type).toList(), withFallback)
                .forEach((name, bytes) -> generatedClasses.produce(new GeneratedClassBuildItem(true, name, bytes)));
    }

    @BuildStep
    void registerForReflection(
            HibernateAccessorBuildTimeConfig config,
            List<HibernateAccessorRegistrationBuildItem> accessorBuildItems,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (config.strategy() == HibernateAccessorStrategy.GENERATED) {
            return;
        }
        String reason = getClass().getName();
        for (HibernateAccessorRegistrationBuildItem item : accessorBuildItems) {
            for (AotFieldMetadata field : Stream
                    .concat(item.getFieldReaders().stream(), item.getFieldWriters().stream()).distinct().toList()) {
                reflectiveFields.produce(new ReflectiveFieldBuildItem(reason, field.declaringClass(), field.name()));
            }
            for (AotMethodMetadata getter : item.getGetters()) {
                reflectiveMethods.produce(
                        new ReflectiveMethodBuildItem(reason, getter.declaringClass(), getter.name(), new String[0]));
            }
            for (AotMethodMetadata setter : item.getSetters()) {
                reflectiveMethods.produce(
                        new ReflectiveMethodBuildItem(reason, setter.declaringClass(), setter.name(),
                                GenerationUtil.parameterTypes(setter.descriptor())));
            }
            for (AotConstructorMetadata ctor : item.getConstructors()) {
                reflectiveClasses.produce(
                        ReflectiveClassBuildItem.builder(ctor.declaringClass()).constructors().build());
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    HibernateAccessorFactoryBuildItem accessFactory(
            HibernateAccessorBuildTimeConfig config,
            HibernateAccessorRecorder recorder) {
        switch (config.strategy()) {
            case REFLECTION -> {
                return new HibernateAccessorFactoryBuildItem(recorder.createReflectionFactory());
            }
            case GENERATED_WITH_FALLBACK -> {
                recorder.initAccessorImplFactory(
                        HibernateAccessorGenerator.GENERATED_READER_IMPL,
                        HibernateAccessorGenerator.GENERATED_WRITER_IMPL,
                        HibernateAccessorGenerator.GENERATED_INSTANTIATOR_IMPL);
                return new HibernateAccessorFactoryBuildItem(
                        recorder.createAccessorFactoryWithFallback(
                                HibernateAccessorGenerator.QUARKUS_HIBERNATE_ACCESSOR_FACTORY));
            }
            default -> {
                recorder.initAccessorImplFactory(
                        HibernateAccessorGenerator.GENERATED_READER_IMPL,
                        HibernateAccessorGenerator.GENERATED_WRITER_IMPL,
                        HibernateAccessorGenerator.GENERATED_INSTANTIATOR_IMPL);
                return new HibernateAccessorFactoryBuildItem(
                        recorder.createAccessorFactory(
                                HibernateAccessorGenerator.QUARKUS_HIBERNATE_ACCESSOR_FACTORY));
            }
        }
    }

    static List<ProcessedHostData> mergeRegistrations(List<HibernateAccessorRegistrationBuildItem> registrations) {
        Map<String, ProcessedHostData> byType = new TreeMap<>();
        for (HibernateAccessorRegistrationBuildItem item : registrations) {
            AotTypeMetadata type = item.getType();
            ProcessedHostData data = byType.computeIfAbsent(type.name(), ignored -> new ProcessedHostData(type));
            if (!data.type().equals(type)) {
                throw new IllegalArgumentException("Conflicting accessor hosts or type metadata for " + type.name());
            }
            data.readers().addAll(item.getFieldReaders());
            data.readers().addAll(item.getGetters());
            data.writers().addAll(item.getFieldWriters());
            data.writers().addAll(item.getSetters());
            data.constructors().addAll(item.getConstructors());
        }
        return byType.values().stream().sorted(Comparator.comparing(ProcessedHostData::type)).toList();
    }

    record ProcessedHostData(AotTypeMetadata type,
            Set<AotMemberMetadata> readers,
            Set<AotMemberMetadata> writers,
            Set<AotConstructorMetadata> constructors) {

        private ProcessedHostData(AotTypeMetadata type) {
            this(type, new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
        }
    }
}
