package io.quarkus.hibernate.validator.runtime;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.Path;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import io.quarkus.hibernate.validator.runtime.clockprovider.RuntimeReinitializedDefaultClockProvider;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyConfigSupport;
import io.quarkus.hibernate.validator.runtime.locale.LocaleResolversWrapper;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateValidatorRecorder {

    private static final TypeLiteral<Instance<LocaleResolversWrapper>> INSTANCE_LOCAL_RESOLVER_WRAPPER_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<ConstraintValidatorFactory>> INSTANCE_CONSTRAINT_VALIDATOR_FACTORY_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<MessageInterpolator>> INSTANCE_MESSAGE_INTERPOLATOR_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<TraversableResolver>> INSTANCE_TRAVERSABLE_RESOLVER_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<ParameterNameProvider>> INSTANCE_PARAMETER_NAME_PROVIDER_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<ClockProvider>> INSTANCE_CLOCK_PROVIDER_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<ScriptEvaluatorFactory>> INSTANCE_SCRIPT_EVALUATOR_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<GetterPropertySelectionStrategy>> INSTANCE_GETTER_PROPERTY_SELECTION_STRATEGY_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<PropertyNodeNameProvider>> INSTANCE_PROPERTY_NODE_NAME_PROVIDER_TYPE_LITERAL = new TypeLiteral<>() {
    };
    private static final TypeLiteral<Instance<ValidatorFactoryCustomizer>> INSTANCE_VALIDATOR_FACTORY_CUSTOMER_TYPE_LITERAL = new TypeLiteral<>() {
    };

    public void shutdownConfigValidator(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                ValidatorFactory validatorFactory = HibernateBeanValidationConfigValidator.ConfigValidatorHolder
                        .getValidatorFactory();
                if (validatorFactory != null) {
                    validatorFactory.close();
                }
            }
        });
    }

    public Function<SyntheticCreationalContext<HibernateValidatorFactory>, HibernateValidatorFactory> hibernateValidatorFactory(
            Set<Class<?>> classesToBeValidated,
            Set<String> detectedBuiltinConstraints, Set<Class<?>> valueExtractorClasses,
            boolean hasXmlConfiguration,
            Optional<BiPredicate<Object, String>> attributeLoadedPredicate,
            LocalesBuildTimeConfig localesBuildTimeConfig,
            HibernateValidatorBuildTimeConfig hibernateValidatorBuildTimeConfig) {
        return new Function<>() {
            @Override
            public HibernateValidatorFactory apply(SyntheticCreationalContext<HibernateValidatorFactory> context) {
                PredefinedScopeHibernateValidatorConfiguration configuration = Validation
                        .byProvider(PredefinedScopeHibernateValidator.class)
                        .configure();

                if (!hasXmlConfiguration) {
                    configuration.ignoreXmlConfiguration();
                }

                LocaleResolver localeResolver;
                Instance<LocaleResolversWrapper> configuredLocaleResolver = context.getInjectedReference(
                        INSTANCE_LOCAL_RESOLVER_WRAPPER_TYPE_LITERAL, NamedLiteral.of("locale-resolver-wrapper"));
                if (configuredLocaleResolver.isResolvable()) {
                    localeResolver = configuredLocaleResolver.get();
                    configuration.localeResolver(localeResolver);
                }

                // Filter out classes with incomplete hierarchy
                filterIncompleteClasses(classesToBeValidated);

                configuration.builtinConstraints(detectedBuiltinConstraints)
                        .initializeBeanMetaData(classesToBeValidated)
                        // Locales, Locale ROOT means all locales in this setting.
                        .locales(localesBuildTimeConfig.locales().contains(Locale.ROOT) ? Set.of(Locale.getAvailableLocales())
                                : localesBuildTimeConfig.locales())
                        .defaultLocale(localesBuildTimeConfig.defaultLocale().orElse(Locale.getDefault()))
                        .beanMetaDataClassNormalizer(new ArcProxyBeanMetaDataClassNormalizer());

                if (hibernateValidatorBuildTimeConfig.expressionLanguage().constraintExpressionFeatureLevel().isPresent()) {
                    configuration.constraintExpressionLanguageFeatureLevel(
                            hibernateValidatorBuildTimeConfig.expressionLanguage().constraintExpressionFeatureLevel().get());
                }

                Instance<ConstraintValidatorFactory> configuredConstraintValidatorFactory = context
                        .getInjectedReference(INSTANCE_CONSTRAINT_VALIDATOR_FACTORY_TYPE_LITERAL);
                if (configuredConstraintValidatorFactory.isResolvable()) {
                    configuration.constraintValidatorFactory(configuredConstraintValidatorFactory.get());
                } else {
                    configuration.constraintValidatorFactory(new ArcConstraintValidatorFactoryImpl());
                }

                Instance<MessageInterpolator> configuredMessageInterpolator = context
                        .getInjectedReference(INSTANCE_MESSAGE_INTERPOLATOR_TYPE_LITERAL);
                if (configuredMessageInterpolator.isResolvable()) {
                    configuration.messageInterpolator(configuredMessageInterpolator.get());
                }

                Instance<TraversableResolver> configuredTraversableResolver = context
                        .getInjectedReference(INSTANCE_TRAVERSABLE_RESOLVER_TYPE_LITERAL);
                if (configuredTraversableResolver.isResolvable()) {
                    configuration.traversableResolver(configuredTraversableResolver.get());
                } else {
                    // we still define the one we want to use so that we do not rely on runtime automatic detection
                    if (attributeLoadedPredicate.isPresent()) {
                        configuration.traversableResolver(new DelegatingTraversableResolver(attributeLoadedPredicate.get()));
                    } else {
                        configuration.traversableResolver(new TraverseAllTraversableResolver());
                    }
                }

                Instance<ParameterNameProvider> configuredParameterNameProvider = context
                        .getInjectedReference(INSTANCE_PARAMETER_NAME_PROVIDER_TYPE_LITERAL);
                if (configuredParameterNameProvider.isResolvable()) {
                    configuration.parameterNameProvider(configuredParameterNameProvider.get());
                }

                Instance<ClockProvider> configuredClockProvider = context
                        .getInjectedReference(INSTANCE_CLOCK_PROVIDER_TYPE_LITERAL);
                if (configuredClockProvider.isResolvable()) {
                    configuration.clockProvider(configuredClockProvider.get());
                } else {
                    // If user didn't provide a custom clock provider we want to set our own.
                    // This provider ensure the correct behavior in a native mode as it does not
                    // cache the time zone at a build time.
                    configuration.clockProvider(RuntimeReinitializedDefaultClockProvider.INSTANCE);
                }

                // Hibernate Validator-specific configuration

                configuration.failFast(hibernateValidatorBuildTimeConfig.failFast());
                configuration.allowOverridingMethodAlterParameterConstraint(
                        hibernateValidatorBuildTimeConfig.methodValidation().allowOverridingParameterConstraints());
                configuration.allowParallelMethodsDefineParameterConstraints(
                        hibernateValidatorBuildTimeConfig.methodValidation().allowParameterConstraintsOnParallelMethods());
                configuration.allowMultipleCascadedValidationOnReturnValues(
                        hibernateValidatorBuildTimeConfig.methodValidation().allowMultipleCascadedValidationOnReturnValues());

                Instance<ScriptEvaluatorFactory> configuredScriptEvaluatorFactory = context
                        .getInjectedReference(INSTANCE_SCRIPT_EVALUATOR_TYPE_LITERAL);
                if (configuredScriptEvaluatorFactory.isResolvable()) {
                    configuration.scriptEvaluatorFactory(configuredScriptEvaluatorFactory.get());
                }

                Instance<GetterPropertySelectionStrategy> configuredGetterPropertySelectionStrategy = context
                        .getInjectedReference(INSTANCE_GETTER_PROPERTY_SELECTION_STRATEGY_TYPE_LITERAL);
                if (configuredGetterPropertySelectionStrategy.isResolvable()) {
                    configuration.getterPropertySelectionStrategy(configuredGetterPropertySelectionStrategy.get());
                }

                Instance<PropertyNodeNameProvider> configuredPropertyNodeNameProvider = context
                        .getInjectedReference(INSTANCE_PROPERTY_NODE_NAME_PROVIDER_TYPE_LITERAL);
                if (configuredPropertyNodeNameProvider.isResolvable()) {
                    configuration.propertyNodeNameProvider(configuredPropertyNodeNameProvider.get());
                }

                // Automatically add all the values extractors declared as beans
                for (ValueExtractor<?> valueExtractor : HibernateValidatorRecorder
                        // We cannot do something like `instance(...).select(ValueExtractor.class)`,
                        // because `ValueExtractor` is usually implemented
                        // as a parameterized type with wildcards,
                        // and the CDI spec does not consider such types as bean types.
                        // We work around that by listing all classes implementing `ValueExtractor` at build time,
                        // then retrieving all bean instances implementing those types here.
                        // See https://github.com/quarkusio/quarkus/pull/30447
                        .<ValueExtractor<?>> uniqueBeanInstances(valueExtractorClasses)) {
                    configuration.addValueExtractor(valueExtractor);
                }

                Instance<ValidatorFactoryCustomizer> validatorFactoryCustomizers = context
                        .getInjectedReference(INSTANCE_VALIDATOR_FACTORY_CUSTOMER_TYPE_LITERAL);
                for (ValidatorFactoryCustomizer validatorFactoryCustomizer : validatorFactoryCustomizers) {
                    validatorFactoryCustomizer.customize(configuration);
                }

                ValidatorFactory validatorFactory = configuration.buildValidatorFactory();

                return validatorFactory.unwrap(HibernateValidatorFactory.class);
            }

            /**
             * Filter out classes with incomplete hierarchy
             */
            private void filterIncompleteClasses(Set<Class<?>> classesToBeValidated) {
                Iterator<Class<?>> iterator = classesToBeValidated.iterator();
                while (iterator.hasNext()) {
                    Class<?> clazz = iterator.next();
                    try {
                        // This should trigger a NoClassDefFoundError if the class has an incomplete hierarchy
                        clazz.getCanonicalName();
                    } catch (NoClassDefFoundError e) {
                        iterator.remove();
                    }
                }
            }
        };
    }

    public Function<SyntheticCreationalContext<Validator>, Validator> hibernateValidator(String hibernateValidatorFactoryName) {
        return new Function<>() {
            @Override
            public Validator apply(SyntheticCreationalContext<Validator> context) {
                HibernateValidatorFactory hibernateValidatorFactory = context
                        .getInjectedReference(HibernateValidatorFactory.class, NamedLiteral.of(hibernateValidatorFactoryName));

                return hibernateValidatorFactory.getValidator();
            }
        };
    }

    // Ideally we'd retrieve all instances of a set of bean types
    // simply by calling something like ArcContainer#select(Set<Type>)
    // but that method does not exist.
    // This method acts as a replacement.
    private static <T> Iterable<T> uniqueBeanInstances(Set<Class<?>> classes) {
        Set<String> beanIds = new HashSet<>();
        for (Class<?> clazz : classes) {
            for (InstanceHandle<?> handle : Arc.container().select(clazz).handles()) {
                if (!handle.isAvailable()) {
                    continue;
                }
                // A single bean can have multiple types.
                // To avoid returning duplicate instances of the same bean,
                // we first retrieve all bean IDs, deduplicate those,
                // then retrieve the instance for each bean.
                // Note that just retrieving all instances and putting them in a identity-based Set
                // would not work, because beans can have the dependent pseudo-scope,
                // in which case we'd have two instances of the same bean.
                beanIds.add(handle.getBean().getIdentifier());
            }
        }
        List<T> instances = new ArrayList<>();
        for (String beanId : beanIds) {
            var arcContainer = Arc.container();
            instances.add(arcContainer.instance(arcContainer.<T> bean(beanId)).get());
        }
        return instances;
    }

    public Supplier<ResteasyConfigSupport> resteasyConfigSupportSupplier(boolean jsonDefault) {
        return new Supplier<ResteasyConfigSupport>() {

            @Override
            public ResteasyConfigSupport get() {
                return new ResteasyConfigSupport(jsonDefault);
            }
        };
    }

    // this is done in order to ensure that HibernateValidatorFactory is fully initialized at static init
    // so completely in heap and ready to go when a native image is built
    public void hibernateValidatorFactoryInit(BeanContainer beanContainer) {
        HibernateValidatorFactory hibernateValidatorFactory = beanContainer.beanInstance(HibernateValidatorFactory.class);
    }

    static final class DelegatingTraversableResolver implements TraversableResolver {
        private final BiPredicate<Object, String> attributeLoadedPredicate;

        DelegatingTraversableResolver(BiPredicate<Object, String> attributeLoadedPredicate) {
            this.attributeLoadedPredicate = attributeLoadedPredicate;
        }

        @Override
        public boolean isReachable(Object entity, Path.Node traversableProperty, Class<?> rootBeanType,
                Path pathToTraversableObject, ElementType elementType) {
            return attributeLoadedPredicate.test(entity, traversableProperty.getName());
        }

        @Override
        public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
                Path pathToTraversableObject, ElementType elementType) {
            return true;
        }
    }
}
