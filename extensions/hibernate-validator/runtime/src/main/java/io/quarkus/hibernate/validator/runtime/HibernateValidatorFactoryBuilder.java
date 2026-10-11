package io.quarkus.hibernate.validator.runtime;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.Path;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.spi.nodenameprovider.PropertyNodeNameProvider;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.hibernate.validator.spi.scripting.ScriptEvaluatorFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import io.quarkus.hibernate.validator.runtime.clockprovider.RuntimeReinitializedDefaultClockProvider;
import io.quarkus.hibernate.validator.runtime.locale.LocaleResolversWrapper;
import io.quarkus.hibernate.validator.spi.AttributeLoadedPredicate;
import io.quarkus.runtime.LocalesBuildTimeConfig;

public final class HibernateValidatorFactoryBuilder {

    private HibernateValidatorFactoryBuilder() {
    }

    public static HibernateValidatorFactory build(
            List<String> classesToBeValidated,
            List<String> detectedBuiltinConstraints,
            List<String> valueExtractorClasses,
            boolean hasXmlConfiguration,
            AttributeLoadedPredicate attributeLoadedPredicate,
            LocalesBuildTimeConfig localesBuildTimeConfig,
            HibernateValidatorBuildTimeConfig hibernateValidatorBuildTimeConfig) {

        PredefinedScopeHibernateValidatorConfiguration configuration = Validation
                .byProvider(PredefinedScopeHibernateValidator.class)
                .configure();

        if (!hasXmlConfiguration) {
            configuration.ignoreXmlConfiguration();
        }

        Instance<LocaleResolversWrapper> configuredLocaleResolver = Arc.container()
                .select(LocaleResolversWrapper.class, NamedLiteral.of("locale-resolver-wrapper"));
        if (configuredLocaleResolver.isResolvable()) {
            configuration.localeResolver(configuredLocaleResolver.get());
        }

        Set<Class<?>> classes = resolveClasses(classesToBeValidated);
        filterIncompleteClasses(classes);

        configuration.builtinConstraints(new LinkedHashSet<>(detectedBuiltinConstraints))
                .initializeBeanMetaData(classes)
                // Locales, Locale ROOT means all locales in this setting.
                .locales(localesBuildTimeConfig.locales().contains(Locale.ROOT) ? Set.of(Locale.getAvailableLocales())
                        : localesBuildTimeConfig.locales())
                .defaultLocale(localesBuildTimeConfig.defaultLocale().orElse(Locale.getDefault()))
                .beanMetaDataClassNormalizer(new ArcProxyBeanMetaDataClassNormalizer());

        if (hibernateValidatorBuildTimeConfig.expressionLanguage().constraintExpressionFeatureLevel().isPresent()) {
            configuration.constraintExpressionLanguageFeatureLevel(
                    hibernateValidatorBuildTimeConfig.expressionLanguage().constraintExpressionFeatureLevel().get());
        }

        Instance<ConstraintValidatorFactory> configuredConstraintValidatorFactory = Arc.container()
                .select(ConstraintValidatorFactory.class);
        if (configuredConstraintValidatorFactory.isResolvable()) {
            configuration.constraintValidatorFactory(configuredConstraintValidatorFactory.get());
        } else {
            configuration.constraintValidatorFactory(new ArcConstraintValidatorFactoryImpl());
        }

        Instance<MessageInterpolator> configuredMessageInterpolator = Arc.container()
                .select(MessageInterpolator.class);
        if (configuredMessageInterpolator.isResolvable()) {
            configuration.messageInterpolator(configuredMessageInterpolator.get());
        }

        Instance<TraversableResolver> configuredTraversableResolver = Arc.container()
                .select(TraversableResolver.class);
        if (configuredTraversableResolver.isResolvable()) {
            configuration.traversableResolver(configuredTraversableResolver.get());
        } else {
            // we still define the one we want to use so that we do not rely on runtime automatic detection
            if (attributeLoadedPredicate != null) {
                configuration.traversableResolver(new DelegatingTraversableResolver(attributeLoadedPredicate));
            } else {
                configuration.traversableResolver(new TraverseAllTraversableResolver());
            }
        }

        Instance<ParameterNameProvider> configuredParameterNameProvider = Arc.container()
                .select(ParameterNameProvider.class);
        if (configuredParameterNameProvider.isResolvable()) {
            configuration.parameterNameProvider(configuredParameterNameProvider.get());
        }

        Instance<ClockProvider> configuredClockProvider = Arc.container()
                .select(ClockProvider.class);
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

        Instance<ScriptEvaluatorFactory> configuredScriptEvaluatorFactory = Arc.container()
                .select(ScriptEvaluatorFactory.class);
        if (configuredScriptEvaluatorFactory.isResolvable()) {
            configuration.scriptEvaluatorFactory(configuredScriptEvaluatorFactory.get());
        }

        Instance<GetterPropertySelectionStrategy> configuredGetterPropertySelectionStrategy = Arc.container()
                .select(GetterPropertySelectionStrategy.class);
        if (configuredGetterPropertySelectionStrategy.isResolvable()) {
            configuration.getterPropertySelectionStrategy(configuredGetterPropertySelectionStrategy.get());
        }

        Instance<PropertyNodeNameProvider> configuredPropertyNodeNameProvider = Arc.container()
                .select(PropertyNodeNameProvider.class);
        if (configuredPropertyNodeNameProvider.isResolvable()) {
            configuration.propertyNodeNameProvider(configuredPropertyNodeNameProvider.get());
        }

        // Automatically add all the values extractors declared as beans
        for (ValueExtractor<?> valueExtractor : HibernateValidatorFactoryBuilder
                // We cannot do something like `instance(...).select(ValueExtractor.class)`,
                // because `ValueExtractor` is usually implemented
                // as a parameterized type with wildcards,
                // and the CDI spec does not consider such types as bean types.
                // We work around that by listing all classes implementing `ValueExtractor` at build time,
                // then retrieving all bean instances implementing those types here.
                // See https://github.com/quarkusio/quarkus/pull/30447
                .<ValueExtractor<?>> uniqueBeanInstances(resolveClasses(valueExtractorClasses))) {
            configuration.addValueExtractor(valueExtractor);
        }

        Instance<ValidatorFactoryCustomizer> validatorFactoryCustomizers = Arc.container()
                .select(ValidatorFactoryCustomizer.class);
        for (ValidatorFactoryCustomizer validatorFactoryCustomizer : validatorFactoryCustomizers) {
            validatorFactoryCustomizer.customize(configuration);
        }

        ValidatorFactory validatorFactory = configuration.buildValidatorFactory();

        return validatorFactory.unwrap(HibernateValidatorFactory.class);
    }

    private static Set<Class<?>> resolveClasses(List<String> classNames) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Set<Class<?>> classes = new LinkedHashSet<>();
        for (String className : classNames) {
            try {
                classes.add(Class.forName(className, true, tccl));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to load class " + className, e);
            }
        }
        return classes;
    }

    /**
     * Filter out classes with incomplete hierarchy
     */
    private static void filterIncompleteClasses(Set<Class<?>> classesToBeValidated) {
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

    static final class DelegatingTraversableResolver implements TraversableResolver {
        private final AttributeLoadedPredicate attributeLoadedPredicate;

        DelegatingTraversableResolver(AttributeLoadedPredicate attributeLoadedPredicate) {
            this.attributeLoadedPredicate = attributeLoadedPredicate;
        }

        @Override
        public boolean isReachable(Object entity, Path.Node traversableProperty, Class<?> rootBeanType,
                Path pathToTraversableObject, ElementType elementType) {
            if (entity == null) {
                // The entity can be null if we are validating values, as that's when the validation context does not have the root object,
                //  In other cases we shouldn't even reach the traversable resolver when the validated bean/entity is null.
                return true;
            }
            return attributeLoadedPredicate.test(entity, traversableProperty.getName());
        }

        @Override
        public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
                Path pathToTraversableObject, ElementType elementType) {
            return true;
        }
    }
}
