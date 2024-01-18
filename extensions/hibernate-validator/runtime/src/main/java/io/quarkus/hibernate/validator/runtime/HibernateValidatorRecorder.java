package io.quarkus.hibernate.validator.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.internal.engine.resolver.JPATraversableResolver;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyConfigSupport;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateValidatorRecorder {

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

    public BeanContainerListener initializeValidatorFactory(Set<Class<?>> classesToBeValidated,
            Set<String> detectedBuiltinConstraints, Set<Class<?>> valueExtractorClasses,
            boolean hasXmlConfiguration, boolean jpaInClasspath,
            ShutdownContext shutdownContext, LocalesBuildTimeConfig localesBuildTimeConfig,
            HibernateValidatorBuildTimeConfig hibernateValidatorBuildTimeConfig) {
        BeanContainerListener beanContainerListener = new BeanContainerListener() {

            @Override
            public void created(BeanContainer container) {
                PredefinedScopeHibernateValidatorConfiguration configuration = Validation
                        .byProvider(PredefinedScopeHibernateValidator.class)
                        .configure();

                if (!hasXmlConfiguration) {
                    configuration.ignoreXmlConfiguration();
                }

                LocaleResolver localeResolver = null;
                InstanceHandle<LocaleResolver> configuredLocaleResolver = Arc.container()
                        .instance("locale-resolver-wrapper");
                if (configuredLocaleResolver.isAvailable()) {
                    localeResolver = configuredLocaleResolver.get();
                    configuration.localeResolver(localeResolver);
                }

                configuration.builtinConstraints(detectedBuiltinConstraints)
                        .initializeBeanMetaData(classesToBeValidated)
                        // Locales, Locale ROOT means all locales in this setting.
                        .locales(localesBuildTimeConfig.locales.contains(Locale.ROOT) ? Set.of(Locale.getAvailableLocales())
                                : localesBuildTimeConfig.locales)
                        .defaultLocale(localesBuildTimeConfig.defaultLocale)
                        .beanMetaDataClassNormalizer(new ArcProxyBeanMetaDataClassNormalizer());

                if (hibernateValidatorBuildTimeConfig.expressionLanguage().constraintExpressionFeatureLevel().isPresent()) {
                    configuration.constraintExpressionLanguageFeatureLevel(
                            hibernateValidatorBuildTimeConfig.expressionLanguage().constraintExpressionFeatureLevel().get());
                }

                InstanceHandle<ConstraintValidatorFactory> configuredConstraintValidatorFactory = Arc.container()
                        .instance(ConstraintValidatorFactory.class);
                if (configuredConstraintValidatorFactory.isAvailable()) {
                    configuration.constraintValidatorFactory(configuredConstraintValidatorFactory.get());
                } else {
                    configuration.constraintValidatorFactory(new ArcConstraintValidatorFactoryImpl());
                }

                InstanceHandle<MessageInterpolator> configuredMessageInterpolator = Arc.container()
                        .instance(MessageInterpolator.class);
                if (configuredMessageInterpolator.isAvailable()) {
                    configuration.messageInterpolator(configuredMessageInterpolator.get());
                }

                InstanceHandle<TraversableResolver> configuredTraversableResolver = Arc.container()
                        .instance(TraversableResolver.class);
                if (configuredTraversableResolver.isAvailable()) {
                    configuration.traversableResolver(configuredTraversableResolver.get());
                } else {
                    // we still define the one we want to use so that we do not rely on runtime automatic detection
                    if (jpaInClasspath) {
                        configuration.traversableResolver(new JPATraversableResolver());
                    } else {
                        configuration.traversableResolver(new TraverseAllTraversableResolver());
                    }
                }

                InstanceHandle<ParameterNameProvider> configuredParameterNameProvider = Arc.container()
                        .instance(ParameterNameProvider.class);
                if (configuredParameterNameProvider.isAvailable()) {
                    configuration.parameterNameProvider(configuredParameterNameProvider.get());
                }

                InstanceHandle<ClockProvider> configuredClockProvider = Arc.container().instance(ClockProvider.class);
                if (configuredClockProvider.isAvailable()) {
                    configuration.clockProvider(configuredClockProvider.get());
                }

                // Hibernate Validator-specific configuration

                configuration.failFast(hibernateValidatorBuildTimeConfig.failFast());
                configuration.allowOverridingMethodAlterParameterConstraint(
                        hibernateValidatorBuildTimeConfig.methodValidation().allowOverridingParameterConstraints());
                configuration.allowParallelMethodsDefineParameterConstraints(
                        hibernateValidatorBuildTimeConfig.methodValidation().allowParameterConstraintsOnParallelMethods());
                configuration.allowMultipleCascadedValidationOnReturnValues(
                        hibernateValidatorBuildTimeConfig.methodValidation().allowMultipleCascadedValidationOnReturnValues());

                InstanceHandle<ScriptEvaluatorFactory> configuredScriptEvaluatorFactory = Arc.container()
                        .instance(ScriptEvaluatorFactory.class);
                if (configuredScriptEvaluatorFactory.isAvailable()) {
                    configuration.scriptEvaluatorFactory(configuredScriptEvaluatorFactory.get());
                }

                InstanceHandle<GetterPropertySelectionStrategy> configuredGetterPropertySelectionStrategy = Arc.container()
                        .instance(GetterPropertySelectionStrategy.class);
                if (configuredGetterPropertySelectionStrategy.isAvailable()) {
                    configuration.getterPropertySelectionStrategy(configuredGetterPropertySelectionStrategy.get());
                }

                InstanceHandle<PropertyNodeNameProvider> configuredPropertyNodeNameProvider = Arc.container()
                        .instance(PropertyNodeNameProvider.class);
                if (configuredPropertyNodeNameProvider.isAvailable()) {
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

                List<InstanceHandle<ValidatorFactoryCustomizer>> validatorFactoryCustomizers = Arc.container()
                        .listAll(ValidatorFactoryCustomizer.class);
                for (InstanceHandle<ValidatorFactoryCustomizer> validatorFactoryInstanceHandle : validatorFactoryCustomizers) {
                    if (validatorFactoryInstanceHandle.isAvailable()) {
                        final ValidatorFactoryCustomizer validatorFactoryCustomizer = validatorFactoryInstanceHandle.get();
                        validatorFactoryCustomizer.customize(configuration);
                    }
                }

                ValidatorFactory validatorFactory = configuration.buildValidatorFactory();
                ValidatorHolder.initialize(validatorFactory.unwrap(HibernateValidatorFactory.class));

                // Close the ValidatorFactory on shutdown
                shutdownContext.addShutdownTask(new Runnable() {
                    @Override
                    public void run() {
                        validatorFactory.close();
                    }
                });
            }
        };

        return beanContainerListener;
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
}
