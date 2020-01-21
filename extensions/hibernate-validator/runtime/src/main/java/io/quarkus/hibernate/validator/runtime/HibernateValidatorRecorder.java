package io.quarkus.hibernate.validator.runtime;

import java.util.Set;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.spi.messageinterpolation.LocaleResolver;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateValidatorRecorder {

    public BeanContainerListener initializeValidatorFactory(Set<Class<?>> classesToBeValidated,
            ShutdownContext shutdownContext, LocalesBuildTimeConfig localesBuildTimeConfig) {
        BeanContainerListener beanContainerListener = new BeanContainerListener() {

            @Override
            public void created(BeanContainer container) {
                PredefinedScopeHibernateValidatorConfiguration configuration = Validation
                        .byProvider(PredefinedScopeHibernateValidator.class)
                        .configure();
                LocaleResolver localeResolver = null;
                InstanceHandle<LocaleResolver> configuredLocaleResolver = Arc.container()
                        .instance(LocaleResolver.class);
                if (configuredLocaleResolver.isAvailable()) {
                    localeResolver = configuredLocaleResolver.get();
                    configuration.localeResolver(localeResolver);
                }

                configuration
                        .initializeBeanMetaData(classesToBeValidated)
                        .locales(localesBuildTimeConfig.locales)
                        .defaultLocale(localesBuildTimeConfig.defaultLocale)
                        .beanMetaDataClassNormalizer(new ArcProxyBeanMetaDataClassNormalizer());

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

                // Automatically add all the values extractors declared as beans
                for (ValueExtractor<?> valueExtractor : Arc.container().beanManager().createInstance()
                        .select(ValueExtractor.class)) {
                    configuration.addValueExtractor(valueExtractor);
                }

                ValidatorFactory validatorFactory = configuration.buildValidatorFactory();
                ValidatorHolder.initialize(validatorFactory);

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
}
