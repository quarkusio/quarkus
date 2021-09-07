package io.quarkus.arc.processor;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * An integrator should create a new instance of the bean processor using the convenient {@link Builder} and then invoke the
 * "processing" methods in the following order:
 *
 * <ol>
 * <li>{@link #registerCustomContexts()}</li>
 * <li>{@link #registerScopes()}</li>
 * <li>{@link #registerBeans()}</li>
 * <li>{@link #initialize(Consumer)}</li>
 * <li>{@link #validate(Consumer)}</li>
 * <li>{@link #processValidationErrors(io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext)}</li>
 * <li>{@link #generateResources(ReflectionRegistration, Set, Consumer)}</li>
 * </ol>
 */
public class BeanProcessor {

    public static Builder builder() {
        return new Builder();
    }

    static final String DEFAULT_NAME = "Default";

    static final Logger LOGGER = Logger.getLogger(BeanProcessor.class);

    private final String name;
    private final ResourceOutput output;
    private final AnnotationLiteralProcessor annotationLiterals;
    private final ReflectionRegistration reflectionRegistration;
    private final List<BeanRegistrar> beanRegistrars;
    private final List<ContextRegistrar> contextRegistrars;
    private final List<ObserverRegistrar> observerRegistrars;
    private final List<BeanDeploymentValidator> beanDeploymentValidators;
    private final BuildContextImpl buildContext;
    private final Predicate<DotName> applicationClassPredicate;
    private final BeanDeployment beanDeployment;
    private final boolean generateSources;
    private final boolean allowMocking;
    private final boolean transformUnproxyableClasses;

    // This predicate is used to filter annotations for InjectionPoint metadata
    // Note that we do create annotation literals for all annotations for an injection point that resolves to a @Dependent bean that injects the InjectionPoint metadata
    // The original use case is to ignore JDK annotations that would prevent an application built with JDK 9+ from targeting JDK 8
    // Such as java.lang.Deprecated 
    protected final Predicate<DotName> injectionPointAnnotationsPredicate;

    private BeanProcessor(Builder builder) {
        this.reflectionRegistration = builder.reflectionRegistration;
        this.applicationClassPredicate = builder.applicationClassPredicate;
        this.name = builder.name;
        this.output = builder.output;
        this.annotationLiterals = new AnnotationLiteralProcessor(builder.sharedAnnotationLiterals, applicationClassPredicate);
        this.generateSources = builder.generateSources;
        this.allowMocking = builder.allowMocking;
        this.transformUnproxyableClasses = builder.transformUnproxyableClasses;

        // Initialize all build processors
        buildContext = new BuildContextImpl();
        buildContext.putInternal(Key.INDEX.asString(), builder.beanArchiveIndex);

        this.beanRegistrars = initAndSort(builder.beanRegistrars, buildContext);
        this.observerRegistrars = initAndSort(builder.observerRegistrars, buildContext);
        this.contextRegistrars = initAndSort(builder.contextRegistrars, buildContext);
        this.beanDeploymentValidators = initAndSort(builder.beanDeploymentValidators, buildContext);
        this.beanDeployment = new BeanDeployment(buildContext, builder);

        // Make it configurable if we find that the set of annotations needs to grow
        this.injectionPointAnnotationsPredicate = annotationName -> !annotationName.equals(DotNames.DEPRECATED);
    }

    public ContextRegistrar.RegistrationContext registerCustomContexts() {
        return beanDeployment.registerCustomContexts(contextRegistrars);
    }

    public void registerScopes() {
        beanDeployment.registerScopes();
    }

    /**
     * Analyze the deployment and register all beans and observers declared on the classes. Furthermore, register all synthetic
     * beans provided by bean registrars.
     *
     * @return the context applied to {@link BeanRegistrar}
     */
    public BeanRegistrar.RegistrationContext registerBeans() {
        return beanDeployment.registerBeans(beanRegistrars);
    }

    public ObserverRegistrar.RegistrationContext registerSyntheticObservers() {
        return beanDeployment.registerSyntheticObservers(observerRegistrars);
    }

    /**
     *
     * @param bytecodeTransformerConsumer Used to register a bytecode transformation
     * @param additionalUnusedBeanExclusions Additional predicates to exclude unused beans
     */
    public void initialize(Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            List<Predicate<BeanInfo>> additionalUnusedBeanExclusions) {
        beanDeployment.init(bytecodeTransformerConsumer, additionalUnusedBeanExclusions);
    }

    /**
     *
     * @param bytecodeTransformerConsumer Used to register a bytecode transformation
     * @return the validation context
     */
    public BeanDeploymentValidator.ValidationContext validate(Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        return beanDeployment.validate(beanDeploymentValidators, bytecodeTransformerConsumer);
    }

    public void processValidationErrors(BeanDeploymentValidator.ValidationContext validationContext) {
        BeanDeployment.processErrors(validationContext.getDeploymentProblems());
    }

    public List<Resource> generateResources(ReflectionRegistration reflectionRegistration, Set<String> existingClasses,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean detectUnusedFalsePositives)
            throws IOException {
        if (reflectionRegistration == null) {
            reflectionRegistration = this.reflectionRegistration;
        }
        PrivateMembersCollector privateMembers = new PrivateMembersCollector();
        Map<BeanInfo, String> beanToGeneratedName = new HashMap<>();
        Map<ObserverInfo, String> observerToGeneratedName = new HashMap<>();

        BeanGenerator beanGenerator = new BeanGenerator(annotationLiterals, applicationClassPredicate, privateMembers,
                generateSources, reflectionRegistration, existingClasses, beanToGeneratedName,
                injectionPointAnnotationsPredicate);
        ClientProxyGenerator clientProxyGenerator = new ClientProxyGenerator(applicationClassPredicate, generateSources,
                allowMocking, reflectionRegistration, existingClasses);
        InterceptorGenerator interceptorGenerator = new InterceptorGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers, generateSources, reflectionRegistration, existingClasses, beanToGeneratedName,
                injectionPointAnnotationsPredicate);
        DecoratorGenerator decoratorGenerator = new DecoratorGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers, generateSources, reflectionRegistration, existingClasses, beanToGeneratedName,
                injectionPointAnnotationsPredicate);
        SubclassGenerator subclassGenerator = new SubclassGenerator(annotationLiterals, applicationClassPredicate,
                generateSources, reflectionRegistration, existingClasses);
        ObserverGenerator observerGenerator = new ObserverGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers, generateSources, reflectionRegistration, existingClasses, observerToGeneratedName,
                injectionPointAnnotationsPredicate, allowMocking);
        AnnotationLiteralGenerator annotationLiteralsGenerator = new AnnotationLiteralGenerator(generateSources);

        List<Resource> resources = new LinkedList<>();

        // Generate interceptors
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Resource resource : interceptorGenerator.generate(interceptor)) {
                resources.add(resource);
            }
        }
        // Generate decorators
        for (DecoratorInfo decorator : beanDeployment.getDecorators()) {
            for (Resource resource : decoratorGenerator.generate(decorator)) {
                resources.add(resource);
            }
        }
        // Generate beans
        for (BeanInfo bean : beanDeployment.getBeans()) {
            for (Resource resource : beanGenerator.generate(bean)) {
                resources.add(resource);
                if (SpecialType.BEAN.equals(resource.getSpecialType())) {
                    if (bean.getScope().isNormal()) {
                        // Generate client proxy
                        resources.addAll(
                                clientProxyGenerator.generate(bean, resource.getFullyQualifiedName(),
                                        bytecodeTransformerConsumer, transformUnproxyableClasses));
                    }
                    if (bean.isSubclassRequired()) {
                        resources.addAll(
                                subclassGenerator.generate(bean, resource.getFullyQualifiedName()));
                    }
                }
            }
        }

        // Generate observers
        for (ObserverInfo observer : beanDeployment.getObservers()) {
            for (Resource resource : observerGenerator.generate(observer)) {
                resources.add(resource);
            }
        }

        privateMembers.log();

        // Generate _ComponentsProvider
        resources.addAll(
                new ComponentsProviderGenerator(annotationLiterals, generateSources, detectUnusedFalsePositives).generate(name,
                        beanDeployment,
                        beanToGeneratedName,
                        observerToGeneratedName));

        // Generate AnnotationLiterals
        if (annotationLiterals.hasLiteralsToGenerate()) {
            resources.addAll(
                    annotationLiteralsGenerator.generate(name, beanDeployment, annotationLiterals.getCache(), existingClasses));
        }

        if (output != null) {
            for (Resource resource : resources) {
                output.writeResource(resource);
            }
        }
        return resources;
    }

    public BeanDeployment getBeanDeployment() {
        return beanDeployment;
    }

    public AnnotationLiteralProcessor getAnnotationLiteralProcessor() {
        return annotationLiterals;
    }

    public BeanDeployment process() throws IOException {
        Consumer<BytecodeTransformer> unsupportedBytecodeTransformer = new Consumer<BytecodeTransformer>() {
            @Override
            public void accept(BytecodeTransformer transformer) {
                throw new UnsupportedOperationException();
            }
        };
        registerCustomContexts();
        registerScopes();
        registerBeans();
        registerSyntheticObservers();
        initialize(unsupportedBytecodeTransformer, Collections.emptyList());
        ValidationContext validationContext = validate(unsupportedBytecodeTransformer);
        processValidationErrors(validationContext);
        generateResources(null, new HashSet<>(), unsupportedBytecodeTransformer, true);
        return beanDeployment;
    }

    public static class Builder {

        String name;
        IndexView beanArchiveIndex;
        IndexView applicationIndex;
        Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations;
        Map<DotName, Collection<AnnotationInstance>> additionalStereotypes;
        ResourceOutput output;
        boolean sharedAnnotationLiterals;
        ReflectionRegistration reflectionRegistration;

        final List<DotName> resourceAnnotations;
        final List<AnnotationsTransformer> annotationTransformers;
        final List<InjectionPointsTransformer> injectionPointTransformers;
        final List<ObserverTransformer> observerTransformers;
        final List<BeanRegistrar> beanRegistrars;
        final List<ObserverRegistrar> observerRegistrars;
        final List<ContextRegistrar> contextRegistrars;
        final List<QualifierRegistrar> qualifierRegistrars;
        final List<InterceptorBindingRegistrar> interceptorBindingRegistrars;
        final List<BeanDeploymentValidator> beanDeploymentValidators;

        boolean removeUnusedBeans = false;
        final List<Predicate<BeanInfo>> removalExclusions;

        boolean generateSources;
        boolean jtaCapabilities;
        boolean transformUnproxyableClasses;
        boolean allowMocking;

        AlternativePriorities alternativePriorities;
        final List<Predicate<ClassInfo>> excludeTypes;

        Predicate<DotName> applicationClassPredicate;

        public Builder() {
            name = DEFAULT_NAME;
            additionalBeanDefiningAnnotations = Collections.emptySet();
            additionalStereotypes = Collections.emptyMap();
            sharedAnnotationLiterals = true;
            reflectionRegistration = ReflectionRegistration.NOOP;
            resourceAnnotations = new ArrayList<>();
            annotationTransformers = new ArrayList<>();
            injectionPointTransformers = new ArrayList<>();
            observerTransformers = new ArrayList<>();
            beanRegistrars = new ArrayList<>();
            observerRegistrars = new ArrayList<>();
            contextRegistrars = new ArrayList<>();
            qualifierRegistrars = new ArrayList<>();
            interceptorBindingRegistrars = new ArrayList<>();
            beanDeploymentValidators = new ArrayList<>();

            removeUnusedBeans = false;
            removalExclusions = new ArrayList<>();

            generateSources = false;
            jtaCapabilities = false;
            transformUnproxyableClasses = false;
            allowMocking = false;

            excludeTypes = new ArrayList<>();

            applicationClassPredicate = dn -> true;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the bean archive index. This index is mandatory and is used to discover components (beans, interceptors,
         * qualifiers, etc.) and during type-safe resolution.
         * 
         * @param beanArchiveIndex
         * @return self
         */
        public Builder setBeanArchiveIndex(IndexView beanArchiveIndex) {
            this.beanArchiveIndex = beanArchiveIndex;
            return this;
        }

        /**
         * Set the application index. This index is optional and is also used to discover types during type-safe resolution.
         * <p>
         * Some types may not be part of the bean archive index but are still needed during type-safe resolution.
         * 
         * @param applicationIndex
         * @return self
         */
        public Builder setApplicationIndex(IndexView applicationIndex) {
            this.applicationIndex = applicationIndex;
            return this;
        }

        public Builder setAdditionalBeanDefiningAnnotations(
                Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations) {
            Objects.requireNonNull(additionalBeanDefiningAnnotations);
            this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
            return this;
        }

        public Builder setAdditionalStereotypes(Map<DotName, Collection<AnnotationInstance>> additionalStereotypes) {
            Objects.requireNonNull(additionalStereotypes);
            this.additionalStereotypes = additionalStereotypes;
            return this;
        }

        public Builder addQualifierRegistrar(QualifierRegistrar qualifierRegistrar) {
            this.qualifierRegistrars.add(qualifierRegistrar);
            return this;
        }

        public Builder addInterceptorBindingRegistrar(InterceptorBindingRegistrar bindingRegistrar) {
            this.interceptorBindingRegistrars.add(bindingRegistrar);
            return this;
        }

        public Builder setOutput(ResourceOutput output) {
            this.output = output;
            return this;
        }

        public Builder setSharedAnnotationLiterals(boolean sharedAnnotationLiterals) {
            this.sharedAnnotationLiterals = sharedAnnotationLiterals;
            return this;
        }

        public Builder setReflectionRegistration(ReflectionRegistration reflectionRegistration) {
            this.reflectionRegistration = reflectionRegistration;
            return this;
        }

        public Builder addAnnotationTransformer(AnnotationsTransformer transformer) {
            this.annotationTransformers.add(transformer);
            return this;
        }

        public Builder addInjectionPointTransformer(InjectionPointsTransformer transformer) {
            this.injectionPointTransformers.add(transformer);
            return this;
        }

        public Builder addObserverTransformer(ObserverTransformer transformer) {
            this.observerTransformers.add(transformer);
            return this;
        }

        public Builder addResourceAnnotations(Collection<DotName> resourceAnnotations) {
            this.resourceAnnotations.addAll(resourceAnnotations);
            return this;
        }

        public Builder addBeanRegistrar(BeanRegistrar registrar) {
            this.beanRegistrars.add(registrar);
            return this;
        }

        public Builder addObserverRegistrar(ObserverRegistrar registrar) {
            this.observerRegistrars.add(registrar);
            return this;
        }

        public Builder addContextRegistrar(ContextRegistrar registrar) {
            this.contextRegistrars.add(registrar);
            return this;
        }

        public Builder addBeanDeploymentValidator(BeanDeploymentValidator validator) {
            this.beanDeploymentValidators.add(validator);
            return this;
        }

        public Builder setApplicationClassPredicate(Predicate<DotName> applicationClassPredicate) {
            this.applicationClassPredicate = applicationClassPredicate;
            return this;
        }

        public Builder setJtaCapabilities(boolean jtaCapabilities) {
            this.jtaCapabilities = jtaCapabilities;
            return this;
        }

        public void setAllowMocking(boolean allowMocking) {
            this.allowMocking = allowMocking;
        }

        /**
         * If set to true the container will attempt to remove all unused beans.
         * <p>
         * An unused bean:
         * <ul>
         * <li>is not a built-in bean or interceptor,</li>
         * <li>is not eligible for injection to any injection point,</li>
         * <li>is not excluded - see {@link #addRemovalExclusion(Predicate)},</li>
         * <li>does not have a name,</li>
         * <li>does not declare an observer,</li>
         * <li>does not declare any producer which is eligible for injection to any injection point,</li>
         * <li>is not directly eligible for injection into any {@link javax.enterprise.inject.Instance} injection point</li>
         * </ul>
         *
         * @param removeUnusedBeans
         * @return self
         */
        public Builder setRemoveUnusedBeans(boolean removeUnusedBeans) {
            this.removeUnusedBeans = removeUnusedBeans;
            return this;
        }

        /**
         * Exclude unused beans that match the given predicate from removal.
         *
         * @param predicate
         * @return self
         * @see #setRemoveUnusedBeans(boolean)
         */
        public Builder addRemovalExclusion(Predicate<BeanInfo> predicate) {
            this.removalExclusions.add(predicate);
            return this;
        }

        /**
         * If set to true the container will transform unproxyable bean classes during validation.
         *
         * @param value
         * @return self
         */
        public Builder setTransformUnproxyableClasses(boolean value) {
            this.transformUnproxyableClasses = value;
            return this;
        }

        /**
         * If set to true the will generate source files of all generated classes for debug purposes. The generated source is
         * not actually a source file but a textual representation of generated code.
         *
         * @param value
         * @return self
         */
        public Builder setGenerateSources(boolean value) {
            this.generateSources = value;
            return this;
        }

        /**
         * Can be used to compute a priority of an alternative bean. A non-null computed value always
         * takes precedence over the priority defined by {@link Priority}, {@link AlternativePriority} or an alternative
         * stereotype.
         *
         * @param priorities
         * @return self
         */
        public Builder setAlternativePriorities(AlternativePriorities priorities) {
            this.alternativePriorities = priorities;
            return this;
        }

        /**
         * Specify the types that should be excluded from discovery.
         * 
         * @param predicate
         * @return self
         */
        public Builder addExcludeType(Predicate<ClassInfo> predicate) {
            this.excludeTypes.add(predicate);
            return this;
        }

        public BeanProcessor build() {
            return new BeanProcessor(this);
        }

    }

    static <E extends BuildExtension> List<E> initAndSort(List<E> extensions, BuildContext buildContext) {
        for (Iterator<E> iterator = extensions.iterator(); iterator.hasNext();) {
            if (!iterator.next().initialize(buildContext)) {
                iterator.remove();
            }
        }
        extensions.sort(BuildExtension::compare);
        return extensions;
    }

    static class BuildContextImpl implements BuildContext {

        private final Map<String, Object> data = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <V> V get(Key<V> key) {
            return (V) data.get(key.asString());
        }

        @Override
        public <V> V put(Key<V> key, V value) {
            String keyStr = key.asString();
            if (keyStr.startsWith(Key.BUILT_IN_PREFIX)) {
                throw new IllegalArgumentException("Key may not start wit " + Key.BUILT_IN_PREFIX + ": " + keyStr);
            }
            return putInternal(keyStr, value);
        }

        @SuppressWarnings("unchecked")
        <V> V putInternal(String key, V value) {
            return (V) data.put(key, value);
        }

    }

    static class PrivateMembersCollector {

        private final List<String> appDescriptions;
        private final List<String> fwkDescriptions;

        public PrivateMembersCollector() {
            this.appDescriptions = new ArrayList<>();
            this.fwkDescriptions = LOGGER.isDebugEnabled() ? new ArrayList<>() : null;
        }

        void add(boolean isApplicationClass, String description) {
            if (isApplicationClass) {
                appDescriptions.add(description);
            } else if (fwkDescriptions != null) {
                fwkDescriptions.add(description);
            }
        }

        private void log() {
            // Log application problems
            if (!appDescriptions.isEmpty()) {
                int limit = LOGGER.isDebugEnabled() ? Integer.MAX_VALUE : 3;
                String info = appDescriptions.stream().limit(limit).map(d -> "\t- " + d).collect(Collectors.joining(",\n"));
                if (appDescriptions.size() > limit) {
                    info += "\n\t- and " + (appDescriptions.size() - limit)
                            + " more - please enable debug logging to see the full list";
                }
                LOGGER.infof(
                        "Found unrecommended usage of private members (use package-private instead) in application beans:%n%s",
                        info);
            }
            // Log fwk problems
            if (fwkDescriptions != null && !fwkDescriptions.isEmpty()) {
                LOGGER.debugf(
                        "Found unrecommended usage of private members (use package-private instead) in framework beans:%n%s",
                        fwkDescriptions.stream().map(d -> "\t- " + d).collect(Collectors.joining(",\n")));
            }
        }

    }

}
