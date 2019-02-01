package org.jboss.shamrock.deployment;

import static org.jboss.shamrock.deployment.util.ReflectUtil.isBuildProducerOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.isConsumerOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.isListOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.isOptionalOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.isSupplierOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.isSupplierOfOptionalOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.rawTypeExtends;
import static org.jboss.shamrock.deployment.util.ReflectUtil.rawTypeIs;
import static org.jboss.shamrock.deployment.util.ReflectUtil.rawTypeOf;
import static org.jboss.shamrock.deployment.util.ReflectUtil.rawTypeOfParameter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildStepBuilder;
import org.jboss.builder.ConsumeFlag;
import org.jboss.builder.ConsumeFlags;
import org.jboss.builder.ProduceFlag;
import org.jboss.builder.ProduceFlags;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import org.jboss.shamrock.deployment.builditem.CapabilityBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationRegistrationBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationRunTimeKeyBuildItem;
import org.jboss.shamrock.deployment.builditem.MainBytecodeRecorderBuildItem;
import org.jboss.shamrock.deployment.builditem.StaticBytecodeRecorderBuildItem;
import org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.deployment.util.ReflectUtil;
import org.jboss.shamrock.deployment.util.StringUtil;
import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationBuildItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;
import org.jboss.shamrock.runtime.annotations.Template;
import org.wildfly.common.function.Functions;

/**
 * Utility class to load build steps from a given extension class.
 */
public final class ExtensionStepLoader {
    private ExtensionStepLoader() {}

    private static String nameOf(AnnotatedElement element) {
        if (element instanceof Parameter) {
            return ((Parameter) element).getName();
        } else if (element instanceof Member) {
            return ((Member) element).getName();
        } else if (element instanceof Class<?>) {
            return ((Class) element).getName();
        } else {
            return null;
        }
    }

    private static String addressOfConfig(AnnotatedElement element) {
        final ConfigItem itemAnnotation = element.getAnnotation(ConfigItem.class);
        if (itemAnnotation == null) {
            return StringUtil.hyphenate(nameOf(element));
        }
        String name = itemAnnotation.name();
        boolean hyphName = name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME);
        boolean elemName = name.equals(ConfigItem.ELEMENT_NAME);
        if (elemName || hyphName) {
            final String elementName = nameOf(element);
            name = hyphName ? StringUtil.hyphenate(elementName) : elementName;
        }
        return name;
    }

    private static boolean isTemplate(AnnotatedElement element) {
        return element.isAnnotationPresent(Template.class);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader) throws IOException, ClassNotFoundException {
        Consumer<BuildChainBuilder> result = Functions.discardingConsumer();
        final Enumeration<URL> resources = classLoader.getResources("META-INF/shamrock-build-steps.list");
        final Set<Class<?>> found = new HashSet<>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            try (InputStream is = url.openStream()) {
                try (BufferedInputStream bis = new BufferedInputStream(is)) {
                    try (InputStreamReader isr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                        try (BufferedReader br = new BufferedReader(isr)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.trim();
                                final Class<?> clazz = Class.forName(line, true, classLoader);
                                if (found.add(clazz)) {
                                    result = result.andThen(ExtensionStepLoader.loadStepsFrom(clazz));
                                }
                            }
                        }
                    }
                }
            }
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
        // this is the list of all configuration root types that are injected by this step
        final List<ConfigurationRegistrationBuildItem> registrationItems = new ArrayList<>();

        if (constructors.length != 1) {
            throw reportError(clazz, "Build step classes must have exactly one constructor");
        }

        boolean consumingConfig = false;

        final Constructor<?> constructor = constructors[0];
        if (! (Modifier.isPublic(constructor.getModifiers()))) constructor.setAccessible(true);
        final Parameter[] ctorParameters = constructor.getParameters();
        final List<Function<BuildContext, Object>> ctorParamFns;
        if (ctorParameters.length == 0) {
            ctorParamFns = Collections.emptyList();
        } else {
            ctorParamFns = new ArrayList<>(ctorParameters.length);
            for (Parameter parameter : ctorParameters) {
                Type parameterType = parameter.getParameterizedType();
                if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(parameterType).asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consume(buildItemClass));
                } else if (isListOf(parameterType, MultiBuildItem.class)) {
                    final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(MultiBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consumeMulti(buildItemClass));
                } else if (isConsumerOf(parameterType, BuildItem.class)) {
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(BuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    ctorParamFns.add(bc -> (Consumer<? extends BuildItem>) bc::produce);
                } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(BuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    ctorParamFns.add(bc -> (BuildProducer<? extends BuildItem>) bc::produce);
                } else if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                    ctorParamFns.add(bc -> Optional.ofNullable(bc.consume(buildItemClass)));
                } else if (isSupplierOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass));
                } else if (isSupplierOfOptionalOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(rawTypeOfParameter(parameterType, 0), 0).asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                    ctorParamFns.add(bc -> (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional.ofNullable(bc.consume(buildItemClass)));
                } else if (rawTypeOf(parameterType) == Executor.class) {
                    ctorParamFns.add(BuildContext::getExecutor);
                } else if (parameter.getType().isAnnotationPresent(ConfigGroup.class) || parameter.getType() == Map.class && rawTypeOfParameter(parameterType, 0) == String.class) {
                    consumingConfig = true;
                    final ConfigRoot configRootAnnotation = parameter.getType().getAnnotation(ConfigRoot.class);
                    final EnumSet<ConfigPhase> phases = phasesOf(configRootAnnotation);
                    String address = addressOfConfig(parameter);
                    registrationItems.add(new ConfigurationRegistrationBuildItem(parameter.getParameterizedType(), address, parameter));
                    ctorParamFns.add(bc -> bc.consume(ConfigurationBuildItem.class).getConfigurationObject(address));
                    if (phases.contains(ConfigPhase.STATIC_INIT) || phases.contains(ConfigPhase.MAIN)) {
                        stepInstanceSetup = stepInstanceSetup.andThen((bc, o) ->
                            bc.produce(new ConfigurationRunTimeKeyBuildItem(
                                address,
                                phases,
                                bc.consume(ConfigurationBuildItem.class).getConfigDefinition().getRootType(address))
                            )
                        );
                    }
                } else if (isTemplate(parameter.getType())) {
                    throw reportError(parameter, "Bytecode recording templates disallowed on constructor parameters");
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
            if (! Modifier.isPublic(mods) || ! Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                field.setAccessible(true);
            }
            // next, determine the type
            final Type fieldType = field.getGenericType();
            if (rawTypeExtends(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(fieldType).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consume(buildItemClass)));
            } else if (isListOf(fieldType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(MultiBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consumeMulti(buildItemClass)));
            } else if (isConsumerOf(fieldType, BuildItem.class)) {
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (Consumer<? extends BuildItem>) bc::produce));
            } else if (isBuildProducerOf(fieldType, BuildItem.class)) {
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (BuildProducer<? extends BuildItem>) bc::produce));
            } else if (isOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, Optional.ofNullable(bc.consume(buildItemClass))));
            } else if (isSupplierOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass)));
            } else if (isSupplierOfOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(rawTypeOfParameter(fieldType, 0), 0).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional.ofNullable(bc.consume(buildItemClass))));
            } else if (field.getType() == Executor.class) {
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.getExecutor()));
            } else if (field.getType().isAnnotationPresent(ConfigGroup.class) || field.getType() == Map.class && rawTypeOfParameter(fieldType, 0) == String.class) {
                consumingConfig = true;
                final ConfigRoot configRootAnnotation = field.getType().getAnnotation(ConfigRoot.class);
                final EnumSet<ConfigPhase> phases = phasesOf(configRootAnnotation);
                String address = addressOfConfig(field);
                registrationItems.add(new ConfigurationRegistrationBuildItem(field.getGenericType(), address, field));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                    final ConfigurationBuildItem configurationBuildItem = bc.consume(ConfigurationBuildItem.class);
                    if (phases.contains(ConfigPhase.STATIC_INIT) || phases.contains(ConfigPhase.MAIN)) {
                        bc.produce(new ConfigurationRunTimeKeyBuildItem(
                            address,
                            phases,
                            bc.consume(ConfigurationBuildItem.class).getConfigDefinition().getRootType(address))
                        );
                    }
                    ReflectUtil.setFieldVal(field, o, configurationBuildItem.getConfigurationObject(address));
                });
            } else if (isTemplate(field.getType())) {
                throw reportError(field, "Bytecode recording templates disallowed on fields");
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
            if (! method.isAnnotationPresent(BuildStep.class)) continue;
            if (! Modifier.isPublic(mods) || ! Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
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
                    executionTime == ExecutionTime.STATIC_INIT ? StaticBytecodeRecorderBuildItem.class : MainBytecodeRecorderBuildItem.class,
                    optional ? ProduceFlags.of(ProduceFlag.WEAK) : ProduceFlags.NONE
                ));
            }
            boolean methodConsumingConfig = consumingConfig;
            if (methodParameters.length == 0) {
                methodParamFns = Collections.emptyList();
            } else {
                methodParamFns = new ArrayList<>(methodParameters.length);
                for (Parameter parameter : methodParameters) {
                    final Type parameterType = parameter.getParameterizedType();
                    final Class<?> parameterClass = parameter.getType();
                    if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = parameterClass.asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> bc.consume(buildItemClass));
                    } else if (isListOf(parameterType, MultiBuildItem.class)) {
                        final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(MultiBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> bc.consumeMulti(buildItemClass));
                    } else if (isConsumerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(BuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        methodParamFns.add((bc, bri) -> (Consumer<? extends BuildItem>) bc::produce);
                    } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(BuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        methodParamFns.add((bc, bri) -> (BuildProducer<? extends BuildItem>) bc::produce);
                    } else if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                        methodParamFns.add((bc, bri) -> Optional.ofNullable(bc.consume(buildItemClass)));
                    } else if (isSupplierOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0).asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass));
                    } else if (isSupplierOfOptionalOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(rawTypeOfParameter(parameterType, 0), 0).asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                        methodParamFns.add((bc, bri) -> (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional.ofNullable(bc.consume(buildItemClass)));
                    } else if (rawTypeOf(parameterType) == Executor.class) {
                        methodParamFns.add((bc, bri) -> bc.getExecutor());
                    } else if (parameterClass.isAnnotationPresent(ConfigGroup.class) || parameterClass == Map.class && rawTypeOfParameter(parameterType, 0) == String.class) {
                        methodConsumingConfig = true;
                        final ConfigRoot configRootAnnotation = parameterClass.getAnnotation(ConfigRoot.class);
                        final EnumSet<ConfigPhase> phases = phasesOf(configRootAnnotation);
                        String address = addressOfConfig(parameter);
                        registrationItems.add(new ConfigurationRegistrationBuildItem(parameter.getParameterizedType(), address, parameter));
                        methodParamFns.add((bc, bri) -> {
                            final ConfigurationBuildItem configurationBuildItem = bc.consume(ConfigurationBuildItem.class);
                            if (phases.contains(ConfigPhase.STATIC_INIT) || phases.contains(ConfigPhase.MAIN)) {
                                bc.produce(new ConfigurationRunTimeKeyBuildItem(
                                    address,
                                    phases,
                                    bc.consume(ConfigurationBuildItem.class).getConfigDefinition().getRootType(address))
                                );
                            }
                            return configurationBuildItem.getConfigurationObject(address);
                        });
                    } else if (isTemplate(parameter.getType())) {
                        if (! isRecorder) {
                            throw reportError(parameter, "Cannot pass templates to method which is not annotated with " + Record.class);
                        }
                        methodParamFns.add((bc, bri) -> {
                            assert bri != null;
                            return bri.getRecordingProxy(parameterClass);
                        });
                    } else if (parameter.getType() == RecorderContext.class || parameter.getType() == BytecodeRecorderImpl.class) {
                        if (! isRecorder) {
                            throw reportError(parameter, "Cannot pass recorder context to method which is not annotated with " + Record.class);
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
                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(method.getReturnType().asSubclass(BuildItem.class)));
                resultConsumer = (bc, o) -> { if (o != null) bc.produce((BuildItem) o); };
            } else if (isOptionalOf(returnType, BuildItem.class)) {
                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(rawTypeOfParameter(returnType, 0).asSubclass(BuildItem.class)));
                resultConsumer = (bc, o) -> ((Optional<? extends BuildItem>) o).ifPresent(bc::produce);
            } else if (isListOf(returnType, MultiBuildItem.class)) {
                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(rawTypeOfParameter(returnType, 0).asSubclass(MultiBuildItem.class)));
                resultConsumer = (bc, o) -> { if (o != null) bc.produce((List<? extends MultiBuildItem>) o); };
            } else {
                throw reportError(method, "Unsupported method return type " + returnType);
            }

            if (methodConsumingConfig) {
                methodStepConfig = methodStepConfig
                    .andThen(bsb -> bsb.consumes(ConfigurationBuildItem.class))
                    .andThen(bsb -> bsb.produces(ConfigurationRunTimeKeyBuildItem.class, ProduceFlag.WEAK));
            }

            final Consumer<BuildStepBuilder> finalStepConfig = stepConfig.andThen(methodStepConfig).andThen(BuildStepBuilder::build);
            final BiConsumer<BuildContext, Object> finalStepInstanceSetup = stepInstanceSetup;
            final String name = clazz.getName() + "#" + method.getName();
            chainConfig = chainConfig.andThen(bcb -> finalStepConfig.accept(bcb.addBuildStep(new org.jboss.builder.BuildStep() {
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
                    BytecodeRecorderImpl bri = isRecorder ? new BytecodeRecorderImpl(recordAnnotation.value() == ExecutionTime.STATIC_INIT, clazz.getSimpleName(), method.getName()) : null;
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
        if (! registrationItems.isEmpty()) {
            chainConfig = chainConfig.andThen(bcb -> bcb.addBuildStep(bc -> bc.produce(registrationItems)).produces(ConfigurationRegistrationBuildItem.class).build());
        }
        return chainConfig;
    }

    private static EnumSet<ConfigPhase> phasesOf(final ConfigRoot configRootAnnotation) {
        if (configRootAnnotation == null) {
            return EnumSet.of(ConfigPhase.BUILD);
        } else {
            final EnumSet<ConfigPhase> set = EnumSet.noneOf(ConfigPhase.class);
            Collections.addAll(set, configRootAnnotation.phase());
            return set;
        }
    }

    private static IllegalArgumentException reportError(AnnotatedElement e, String msg) {
        if (e instanceof Member) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Member) e).getDeclaringClass());
        } else if (e instanceof Parameter) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Parameter) e).getDeclaringExecutable() + " of " + ((Parameter) e).getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(msg + " at " + e);
        }
    }
}
