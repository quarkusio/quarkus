package io.quarkus.deployment;

import static io.quarkus.deployment.util.ReflectUtil.isBuildProducerOf;
import static io.quarkus.deployment.util.ReflectUtil.isConsumerOf;
import static io.quarkus.deployment.util.ReflectUtil.isListOf;
import static io.quarkus.deployment.util.ReflectUtil.isOptionalOf;
import static io.quarkus.deployment.util.ReflectUtil.isSupplierOf;
import static io.quarkus.deployment.util.ReflectUtil.isSupplierOfOptionalOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeExtends;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeIs;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOfParameter;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.common.function.Functions;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.ConsumeFlag;
import io.quarkus.builder.ConsumeFlags;
import io.quarkus.builder.ProduceFlag;
import io.quarkus.builder.ProduceFlags;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.BuildTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.BuildTimeRunTimeFixedConfigurationBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.Template;

/**
 * Utility class to load build steps, runtime recorders, and configuration roots from a given extension class.
 */
public final class ExtensionLoader {
    private ExtensionLoader() {
    }

    private static boolean isRecorder(AnnotatedElement element) {
        return element.isAnnotationPresent(Recorder.class) || element.isAnnotationPresent(Template.class);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
        Consumer<BuildChainBuilder> result = Functions.discardingConsumer();
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, "META-INF/quarkus-build-steps.list")) {
            result = result.andThen(ExtensionLoader.loadStepsFrom(clazz));
        }
        return result;
    }

    /**
     * Load all the build steps from the given class.
     *
     * @param clazz the class to load from (must not be {@code null})
     * @return a consumer which adds the steps to the given chain builder
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(Class<?> clazz) {
        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // this is the chain configuration that will contain all steps on this class and be returned
        Consumer<BuildChainBuilder> chainConfig = Functions.discardingConsumer();
        // this is the step configuration that applies to all steps on this class
        Consumer<BuildStepBuilder> stepConfig = Functions.discardingConsumer();
        // this is the build step instance setup that applies to all steps on this class
        BiConsumer<BuildContext, Object> stepInstanceSetup = Functions.discardingBiConsumer();

        if (constructors.length != 1) {
            throw reportError(clazz, "Build step classes must have exactly one constructor");
        }

        EnumSet<ConfigPhase> consumingConfigPhases = EnumSet.noneOf(ConfigPhase.class);

        final Constructor<?> constructor = constructors[0];
        if (!(Modifier.isPublic(constructor.getModifiers())))
            constructor.setAccessible(true);
        final Parameter[] ctorParameters = constructor.getParameters();
        final List<Function<BuildContext, Object>> ctorParamFns;
        if (ctorParameters.length == 0) {
            ctorParamFns = Collections.emptyList();
        } else {
            ctorParamFns = new ArrayList<>(ctorParameters.length);
            for (Parameter parameter : ctorParameters) {
                Type parameterType = parameter.getParameterizedType();
                final Class<?> parameterClass = parameter.getType();
                if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(parameterType)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consume(buildItemClass));
                } else if (isListOf(parameterType, MultiBuildItem.class)) {
                    final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(MultiBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consumeMulti(buildItemClass));
                } else if (isConsumerOf(parameterType, BuildItem.class)) {
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(BuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    ctorParamFns.add(bc -> (Consumer<? extends BuildItem>) bc::produce);
                } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(BuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    ctorParamFns.add(bc -> (BuildProducer<? extends BuildItem>) bc::produce);
                } else if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                    ctorParamFns.add(bc -> Optional.ofNullable(bc.consume(buildItemClass)));
                } else if (isSupplierOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass));
                } else if (isSupplierOfOptionalOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(
                            rawTypeOfParameter(parameterType, 0), 0).asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                    ctorParamFns.add(bc -> (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional
                            .ofNullable(bc.consume(buildItemClass)));
                } else if (rawTypeOf(parameterType) == Executor.class) {
                    ctorParamFns.add(BuildContext::getExecutor);
                } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                    final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                    final ConfigPhase phase = annotation.phase();
                    consumingConfigPhases.add(phase);

                    if (phase == ConfigPhase.BUILD_TIME) {
                        ctorParamFns.add(bc -> bc.consume(BuildTimeConfigurationBuildItem.class).getConfigDefinition()
                                .getRealizedInstance(parameterClass));
                    } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                        ctorParamFns.add(bc -> bc.consume(BuildTimeRunTimeFixedConfigurationBuildItem.class)
                                .getConfigDefinition().getRealizedInstance(parameterClass));
                    } else if (phase == ConfigPhase.RUN_TIME) {
                        ctorParamFns.add(bc -> bc.consume(RunTimeConfigurationBuildItem.class).getConfigDefinition()
                                .getRealizedInstance(parameterClass));
                    } else {
                        throw reportError(parameterClass, "Unknown value for ConfigPhase");
                    }
                } else if (isRecorder(parameterClass)) {
                    throw reportError(parameter, "Bytecode recorders disallowed on constructor parameters");
                } else {
                    throw reportError(parameter, "Unsupported constructor parameter type " + parameterType);
                }
            }
        }

        // index fields
        final Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            final int mods = field.getModifiers();
            if (Modifier.isStatic(mods)) {
                // ignore static fields
                continue;
            }
            if (Modifier.isFinal(mods)) {
                // ignore final fields
                continue;
            }
            if (!Modifier.isPublic(mods) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                field.setAccessible(true);
            }
            // next, determine the type
            final Type fieldType = field.getGenericType();
            final Class<?> fieldClass = field.getType();
            if (rawTypeExtends(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(fieldType).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consume(buildItemClass)));
            } else if (isListOf(fieldType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(MultiBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consumeMulti(buildItemClass)));
            } else if (isConsumerOf(fieldType, BuildItem.class)) {
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (Consumer<? extends BuildItem>) bc::produce));
            } else if (isBuildProducerOf(fieldType, BuildItem.class)) {
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen(
                        (bc, o) -> ReflectUtil.setFieldVal(field, o, (BuildProducer<? extends BuildItem>) bc::produce));
            } else if (isOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, Optional.ofNullable(bc.consume(buildItemClass))));
            } else if (isSupplierOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o,
                        (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass)));
            } else if (isSupplierOfOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(rawTypeOfParameter(fieldType, 0), 0)
                        .asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o,
                        (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional.ofNullable(bc.consume(buildItemClass))));
            } else if (fieldClass == Executor.class) {
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.getExecutor()));
            } else if (fieldClass.isAnnotationPresent(ConfigRoot.class)) {
                final ConfigRoot annotation = fieldClass.getAnnotation(ConfigRoot.class);
                final ConfigPhase phase = annotation.phase();
                consumingConfigPhases.add(phase);

                if (phase == ConfigPhase.BUILD_TIME) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final BuildTimeConfigurationBuildItem configurationBuildItem = bc
                                .consume(BuildTimeConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getConfigDefinition().getRealizedInstance(fieldClass));
                    });
                } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final BuildTimeRunTimeFixedConfigurationBuildItem configurationBuildItem = bc
                                .consume(BuildTimeRunTimeFixedConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getConfigDefinition().getRealizedInstance(fieldClass));
                    });
                } else if (phase == ConfigPhase.RUN_TIME) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final RunTimeConfigurationBuildItem configurationBuildItem = bc
                                .consume(RunTimeConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getConfigDefinition().getRealizedInstance(fieldClass));
                    });
                } else {
                    throw reportError(fieldClass, "Unknown value for ConfigPhase");
                }
            } else if (isRecorder(fieldClass)) {
                throw reportError(field, "Bytecode recorders disallowed on fields");
            } else {
                throw reportError(field, "Unsupported field type " + fieldType);
            }
        }

        // now iterate the methods
        final Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            final int mods = method.getModifiers();
            if (Modifier.isStatic(mods)) {
                continue;
            }
            if (!method.isAnnotationPresent(BuildStep.class))
                continue;
            if (!Modifier.isPublic(mods) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            final BuildStep buildStep = method.getAnnotation(BuildStep.class);
            final String[] archiveMarkers = buildStep.applicationArchiveMarkers();
            final String[] capabilities = buildStep.providesCapabilities();
            final Parameter[] methodParameters = method.getParameters();
            final Record recordAnnotation = method.getAnnotation(Record.class);
            final boolean isRecorder = recordAnnotation != null;
            final List<BiFunction<BuildContext, BytecodeRecorderImpl, Object>> methodParamFns;
            Consumer<BuildStepBuilder> methodStepConfig = Functions.discardingConsumer();
            if (archiveMarkers.length > 0) {
                chainConfig = chainConfig.andThen(bcb -> bcb.addBuildStep(bc -> {
                    for (String marker : archiveMarkers) {
                        bc.produce(new AdditionalApplicationArchiveMarkerBuildItem(marker));
                    }
                }).produces(AdditionalApplicationArchiveMarkerBuildItem.class).build());
            }
            if (capabilities.length > 0) {
                chainConfig = chainConfig.andThen(bcb -> bcb.addBuildStep(bc -> {
                    for (String capability : capabilities) {
                        bc.produce(new CapabilityBuildItem(capability));
                    }
                }).produces(CapabilityBuildItem.class).build());
            }

            if (isRecorder) {
                assert recordAnnotation != null;
                final ExecutionTime executionTime = recordAnnotation.value();
                final boolean optional = recordAnnotation.optional();
                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(
                        executionTime == ExecutionTime.STATIC_INIT ? StaticBytecodeRecorderBuildItem.class
                                : MainBytecodeRecorderBuildItem.class,
                        optional ? ProduceFlags.of(ProduceFlag.WEAK) : ProduceFlags.NONE));
            }
            EnumSet<ConfigPhase> methodConsumingConfigPhases = consumingConfigPhases.clone();
            if (methodParameters.length == 0) {
                methodParamFns = Collections.emptyList();
            } else {
                methodParamFns = new ArrayList<>(methodParameters.length);
                for (Parameter parameter : methodParameters) {
                    final Type parameterType = parameter.getParameterizedType();
                    final Class<?> parameterClass = parameter.getType();
                    if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = parameterClass
                                .asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> bc.consume(buildItemClass));
                    } else if (isListOf(parameterType, MultiBuildItem.class)) {
                        final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(MultiBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> bc.consumeMulti(buildItemClass));
                    } else if (isConsumerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(BuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        methodParamFns.add((bc, bri) -> (Consumer<? extends BuildItem>) bc::produce);
                    } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(BuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        methodParamFns.add((bc, bri) -> (BuildProducer<? extends BuildItem>) bc::produce);
                    } else if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                        methodParamFns.add((bc, bri) -> Optional.ofNullable(bc.consume(buildItemClass)));
                    } else if (isSupplierOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass));
                    } else if (isSupplierOfOptionalOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(
                                rawTypeOfParameter(parameterType, 0), 0).asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                        methodParamFns.add((bc, bri) -> (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional
                                .ofNullable(bc.consume(buildItemClass)));
                    } else if (rawTypeOf(parameterType) == Executor.class) {
                        methodParamFns.add((bc, bri) -> bc.getExecutor());
                    } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                        final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                        final ConfigPhase phase = annotation.phase();
                        methodConsumingConfigPhases.add(phase);

                        if (phase == ConfigPhase.BUILD_TIME) {
                            methodParamFns.add((bc, bri) -> {
                                final BuildTimeConfigurationBuildItem configurationBuildItem = bc
                                        .consume(BuildTimeConfigurationBuildItem.class);
                                return configurationBuildItem.getConfigDefinition().getRealizedInstance(parameterClass);
                            });
                        } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                            methodParamFns.add((bc, bri) -> {
                                final BuildTimeRunTimeFixedConfigurationBuildItem configurationBuildItem = bc
                                        .consume(BuildTimeRunTimeFixedConfigurationBuildItem.class);
                                return configurationBuildItem.getConfigDefinition().getRealizedInstance(parameterClass);
                            });
                        } else if (phase == ConfigPhase.RUN_TIME) {
                            methodParamFns.add((bc, bri) -> {
                                final RunTimeConfigurationBuildItem configurationBuildItem = bc
                                        .consume(RunTimeConfigurationBuildItem.class);
                                return configurationBuildItem.getConfigDefinition().getRealizedInstance(parameterClass);
                            });
                        } else {
                            throw reportError(parameterClass, "Unknown value for ConfigPhase");
                        }
                    } else if (isRecorder(parameter.getType())) {
                        if (!isRecorder) {
                            throw reportError(parameter,
                                    "Cannot pass recorders to method which is not annotated with " + Record.class);
                        }
                        methodParamFns.add((bc, bri) -> {
                            assert bri != null;
                            return bri.getRecordingProxy(parameterClass);
                        });
                    } else if (parameter.getType() == RecorderContext.class
                            || parameter.getType() == BytecodeRecorderImpl.class) {
                        if (!isRecorder) {
                            throw reportError(parameter,
                                    "Cannot pass recorder context to method which is not annotated with " + Record.class);
                        }
                        methodParamFns.add((bc, bri) -> bri);
                    } else {
                        throw reportError(parameter, "Unsupported method parameter " + parameterType);
                    }
                }
            }

            final BiConsumer<BuildContext, Object> resultConsumer;
            final Type returnType = method.getGenericReturnType();
            if (rawTypeIs(returnType, void.class)) {
                resultConsumer = Functions.discardingBiConsumer();
            } else if (rawTypeExtends(returnType, BuildItem.class)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.produces(method.getReturnType().asSubclass(BuildItem.class)));
                resultConsumer = (bc, o) -> {
                    if (o != null)
                        bc.produce((BuildItem) o);
                };
            } else if (isOptionalOf(returnType, BuildItem.class)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.produces(rawTypeOfParameter(returnType, 0).asSubclass(BuildItem.class)));
                resultConsumer = (bc, o) -> ((Optional<? extends BuildItem>) o).ifPresent(bc::produce);
            } else if (isListOf(returnType, MultiBuildItem.class)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.produces(rawTypeOfParameter(returnType, 0).asSubclass(MultiBuildItem.class)));
                resultConsumer = (bc, o) -> {
                    if (o != null)
                        bc.produce((List<? extends MultiBuildItem>) o);
                };
            } else {
                throw reportError(method, "Unsupported method return type " + returnType);
            }

            if (methodConsumingConfigPhases.contains(ConfigPhase.RUN_TIME)) {
                if (isRecorder && recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                    throw reportError(method,
                            "Bytecode recorder is static but an injected config object is declared as run time");
                }
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(RunTimeConfigurationBuildItem.class));
            }
            if (methodConsumingConfigPhases.contains(ConfigPhase.BUILD_AND_RUN_TIME_FIXED)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(BuildTimeRunTimeFixedConfigurationBuildItem.class));
            }
            if (methodConsumingConfigPhases.contains(ConfigPhase.BUILD_TIME)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(BuildTimeConfigurationBuildItem.class));
            }

            final Consumer<BuildStepBuilder> finalStepConfig = stepConfig.andThen(methodStepConfig)
                    .andThen(BuildStepBuilder::build);
            final BiConsumer<BuildContext, Object> finalStepInstanceSetup = stepInstanceSetup;
            final String name = clazz.getName() + "#" + method.getName();
            chainConfig = chainConfig
                    .andThen(bcb -> finalStepConfig.accept(bcb.addBuildStep(new io.quarkus.builder.BuildStep() {
                        public void execute(final BuildContext bc) {
                            Object[] ctorArgs = new Object[ctorParamFns.size()];
                            for (int i = 0; i < ctorArgs.length; i++) {
                                ctorArgs[i] = ctorParamFns.get(i).apply(bc);
                            }
                            Object instance;
                            try {
                                instance = constructor.newInstance(ctorArgs);
                            } catch (InstantiationException e) {
                                throw ReflectUtil.toError(e);
                            } catch (IllegalAccessException e) {
                                throw ReflectUtil.toError(e);
                            } catch (InvocationTargetException e) {
                                try {
                                    throw e.getCause();
                                } catch (RuntimeException | Error e2) {
                                    throw e2;
                                } catch (Throwable t) {
                                    throw new IllegalStateException(t);
                                }
                            }
                            finalStepInstanceSetup.accept(bc, instance);
                            Object[] methodArgs = new Object[methodParamFns.size()];
                            BytecodeRecorderImpl bri = isRecorder
                                    ? new BytecodeRecorderImpl(recordAnnotation.value() == ExecutionTime.STATIC_INIT,
                                            clazz.getSimpleName(), method.getName())
                                    : null;
                            for (int i = 0; i < methodArgs.length; i++) {
                                methodArgs[i] = methodParamFns.get(i).apply(bc, bri);
                            }
                            Object result;
                            try {
                                result = method.invoke(instance, methodArgs);
                            } catch (IllegalAccessException e) {
                                throw ReflectUtil.toError(e);
                            } catch (InvocationTargetException e) {
                                try {
                                    throw e.getCause();
                                } catch (RuntimeException | Error e2) {
                                    throw e2;
                                } catch (Throwable t) {
                                    throw new IllegalStateException(t);
                                }
                            }
                            resultConsumer.accept(bc, result);
                            if (isRecorder) {
                                // commit recorded data
                                if (recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                                    bc.produce(new StaticBytecodeRecorderBuildItem(bri));
                                } else {
                                    bc.produce(new MainBytecodeRecorderBuildItem(bri));
                                }

                            }
                        }

                        public String toString() {
                            return name;
                        }
                    })));
        }
        return chainConfig;
    }

    private static IllegalArgumentException reportError(AnnotatedElement e, String msg) {
        if (e instanceof Member) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Member) e).getDeclaringClass());
        } else if (e instanceof Parameter) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Parameter) e).getDeclaringExecutable() + " of "
                    + ((Parameter) e).getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(msg + " at " + e);
        }
    }
}
