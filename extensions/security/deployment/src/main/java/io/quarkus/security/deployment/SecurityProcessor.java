package io.quarkus.security.deployment;

import java.lang.reflect.Modifier;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanRegistrarBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.runtime.IdentityProviderManagerCreator;
import io.quarkus.security.runtime.SecurityBuildTimeConfig;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.security.runtime.X509IdentityProvider;
import io.quarkus.security.runtime.interceptor.AuthenticatedInterceptor;
import io.quarkus.security.runtime.interceptor.DenyAllInterceptor;
import io.quarkus.security.runtime.interceptor.PermitAllInterceptor;
import io.quarkus.security.runtime.interceptor.RolesAllowedInterceptor;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorage;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.SecurityConstrainer;
import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.runtime.interceptor.check.SecurityCheck;
import io.quarkus.security.spi.AdditionalSecuredClassesBuildIem;
import io.quarkus.security.spi.runtime.AuthorizationController;

public class SecurityProcessor {

    private static final Logger log = Logger.getLogger(SecurityProcessor.class);

    SecurityConfig security;

    /**
     * Register the Elytron-provided password factory SPI implementation
     *
     */
    @BuildStep
    void services(BuildProducer<JCAProviderBuildItem> jcaProviders) {
        // Create JCAProviderBuildItems for any configured provider names
        for (String providerName : security.securityProviders.orElse(Collections.emptyList())) {
            jcaProviders.produce(new JCAProviderBuildItem(providerName));
            log.debugf("Added providerName: %s", providerName);
        }
    }

    @BuildStep
    AdditionalBeanBuildItem authorizationController() {
        return AdditionalBeanBuildItem.builder().addBeanClass(AuthorizationController.class).build();
    }

    /**
     * Register the classes for reflection in the requested named providers
     *
     * @param classes - ReflectiveClassBuildItem producer
     * @param jcaProviders - JCAProviderBuildItem for requested providers
     */
    @BuildStep
    void registerJCAProviders(BuildProducer<ReflectiveClassBuildItem> classes, List<JCAProviderBuildItem> jcaProviders) {
        for (JCAProviderBuildItem provider : jcaProviders) {
            List<String> providerClasses = registerProvider(provider.getProviderName());
            for (String className : providerClasses) {
                classes.produce(new ReflectiveClassBuildItem(true, true, className));
                log.debugf("Register JCA class: %s", className);
            }
        }
    }

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        registrars.produce(new InterceptorBindingRegistrarBuildItem(new SecurityAnnotationsRegistrar()));
        Class[] interceptors = { AuthenticatedInterceptor.class, DenyAllInterceptor.class, PermitAllInterceptor.class,
                RolesAllowedInterceptor.class };
        beans.produce(new AdditionalBeanBuildItem(interceptors));
        beans.produce(new AdditionalBeanBuildItem(SecurityHandler.class, SecurityConstrainer.class));
    }

    /*
     * The annotation store is not meant to be generally supported for security annotation.
     * It is only used here in order to be able to register the DenyAllInterceptor for
     * methods that don't have a security annotation
     */
    @BuildStep
    void transformSecurityAnnotations(BuildProducer<AnnotationsTransformerBuildItem> transformers,
            List<AdditionalSecuredClassesBuildIem> additionalSecuredClasses,
            SecurityBuildTimeConfig config) {
        if (config.denyUnannotated) {
            transformers.produce(new AnnotationsTransformerBuildItem(new DenyingUnannotatedTransformer()));
        }
        if (!additionalSecuredClasses.isEmpty()) {
            Set<String> additionalSecured = new HashSet<>();
            for (AdditionalSecuredClassesBuildIem securedClasses : additionalSecuredClasses) {
                for (ClassInfo additionalSecuredClass : securedClasses.additionalSecuredClasses) {
                    additionalSecured.add(additionalSecuredClass.name().toString());
                }
            }
            transformers.produce(
                    new AnnotationsTransformerBuildItem(new AdditionalDenyingUnannotatedTransformer(additionalSecured)));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void gatherSecurityChecks(BuildProducer<BeanRegistrarBuildItem> beanRegistrars,
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            BuildProducer<ApplicationClassPredicateBuildItem> classPredicate,
            List<AdditionalSecuredClassesBuildIem> additionalSecuredClasses,
            SecurityCheckRecorder recorder,
            List<AdditionalSecurityCheckBuildItem> additionalSecurityChecks, SecurityBuildTimeConfig config) {
        classPredicate.produce(new ApplicationClassPredicateBuildItem(new SecurityCheckStorage.AppPredicate()));

        final Map<DotName, ClassInfo> additionalSecured = new HashMap<>();
        for (AdditionalSecuredClassesBuildIem securedClasses : additionalSecuredClasses) {
            securedClasses.additionalSecuredClasses.forEach(c -> {
                if (!additionalSecured.containsKey(c.name())) {
                    additionalSecured.put(c.name(), c);
                }
            });
        }

        IndexView index = beanArchiveBuildItem.getIndex();
        Map<MethodInfo, SecurityCheck> securityChecks = gatherSecurityAnnotations(
                index, additionalSecured, config.denyUnannotated, recorder);
        for (AdditionalSecurityCheckBuildItem additionalSecurityCheck : additionalSecurityChecks) {
            securityChecks.put(additionalSecurityCheck.getMethodInfo(),
                    additionalSecurityCheck.getSecurityCheck());
        }

        RuntimeValue<SecurityCheckStorageBuilder> builder = recorder.newBuilder();
        for (Map.Entry<MethodInfo, SecurityCheck> methodEntry : securityChecks
                .entrySet()) {
            MethodInfo method = methodEntry.getKey();
            String[] params = new String[method.parameters().size()];
            for (int i = 0; i < method.parameters().size(); ++i) {
                params[i] = method.parameters().get(i).name().toString();
            }
            recorder.addMethod(builder, method.declaringClass().name().toString(), method.name(), params,
                    methodEntry.getValue());
        }
        recorder.create(builder);

        beanRegistrars.produce(new BeanRegistrarBuildItem(new BeanRegistrar() {

            @Override
            public void register(RegistrationContext registrationContext) {

                DotName name = DotName.createSimple(SecurityCheckStorage.class.getName());

                BeanConfigurator<Object> configurator = registrationContext.configure(name);
                configurator.addType(name);
                configurator.scope(BuiltinScope.APPLICATION.getInfo());
                configurator.creator(creator -> {
                    ResultHandle ret = creator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(SecurityCheckRecorder.class, "getStorage",
                                    SecurityCheckStorage.class));
                    creator.returnValue(ret);
                });
                configurator.done();
            }
        }));
    }

    private Map<MethodInfo, SecurityCheck> gatherSecurityAnnotations(
            IndexView index,
            Map<DotName, ClassInfo> additionalSecuredClasses, boolean denyUnannotated, SecurityCheckRecorder recorder) {

        Map<MethodInfo, AnnotationInstance> methodToInstanceCollector = new HashMap<>();
        Map<ClassInfo, AnnotationInstance> classAnnotations = new HashMap<>();
        Map<MethodInfo, SecurityCheck> result = new HashMap<>(gatherSecurityAnnotations(
                index, DotNames.ROLES_ALLOWED, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.rolesAllowed(instance.value().asStringArray()))));
        result.putAll(gatherSecurityAnnotations(index, DotNames.PERMIT_ALL, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.permitAll())));
        result.putAll(gatherSecurityAnnotations(index, DotNames.AUTHENTICATED, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.authenticated())));

        result.putAll(gatherSecurityAnnotations(index, DotNames.DENY_ALL, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.denyAll())));

        /*
         * Handle additional secured classes by adding the denyAll check to all public non-static methods
         * that don't have security annotations
         */
        for (Map.Entry<DotName, ClassInfo> additionalSecureClassInfo : additionalSecuredClasses.entrySet()) {
            for (MethodInfo methodInfo : additionalSecureClassInfo.getValue().methods()) {
                if (!isPublicNonStaticNonConstructor(methodInfo)) {
                    continue;
                }
                AnnotationInstance alreadyExistingInstance = methodToInstanceCollector.get(methodInfo);
                if ((alreadyExistingInstance == null)) {
                    result.put(methodInfo, recorder.denyAll());
                } else if (alreadyExistingInstance.target().kind() == AnnotationTarget.Kind.CLASS) {
                    throw new IllegalStateException("Class " + methodInfo.declaringClass()
                            + " should not have been added as an additional secured class");
                }
            }
        }

        /*
         * If we need to add the denyAll security check to all unannotated methods, we simply go through all secured methods,
         * collect the declaring classes, then go through all methods of the classes and add the necessary check
         */
        if (denyUnannotated) {
            Set<ClassInfo> allClassesWithSecurityChecks = new HashSet<>(methodToInstanceCollector.keySet().size());
            for (MethodInfo methodInfo : methodToInstanceCollector.keySet()) {
                allClassesWithSecurityChecks.add(methodInfo.declaringClass());
            }
            for (ClassInfo classWithSecurityCheck : allClassesWithSecurityChecks) {
                for (MethodInfo methodInfo : classWithSecurityCheck.methods()) {
                    if (!isPublicNonStaticNonConstructor(methodInfo)) {
                        continue;
                    }
                    if (methodToInstanceCollector.containsKey(methodInfo)) { // the method already has a security check
                        continue;
                    }
                    result.put(methodInfo, recorder.denyAll());
                }
            }
        }

        return result;
    }

    private boolean isPublicNonStaticNonConstructor(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && !"<init>".equals(methodInfo.name());
    }

    private Map<MethodInfo, SecurityCheck> gatherSecurityAnnotations(
            IndexView index, DotName dotName,
            Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods,
            Map<ClassInfo, AnnotationInstance> classLevelAnnotations,
            Function<AnnotationInstance, SecurityCheck> securityCheckInstanceCreator) {

        Map<MethodInfo, SecurityCheck> result = new HashMap<>();

        Collection<AnnotationInstance> instances = index.getAnnotations(dotName);
        // make sure we process annotations on methods first
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo methodInfo = target.asMethod();
                if (alreadyCheckedMethods.containsKey(methodInfo)) {
                    throw new IllegalStateException("Method " + methodInfo.name() + " of class " + methodInfo.declaringClass()
                            + " is annotated with multiple security annotations");
                }
                alreadyCheckedMethods.put(methodInfo, instance);
                result.put(methodInfo, securityCheckInstanceCreator.apply(instance));
            }
        }
        // now add the class annotations to methods if they haven't already been annotated
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                List<MethodInfo> methods = target.asClass().methods();
                AnnotationInstance existingClassInstance = classLevelAnnotations.get(target.asClass());
                if (existingClassInstance == null) {
                    classLevelAnnotations.put(target.asClass(), instance);
                    for (MethodInfo methodInfo : methods) {
                        AnnotationInstance alreadyExistingInstance = alreadyCheckedMethods.get(methodInfo);
                        if ((alreadyExistingInstance == null)) {
                            result.put(methodInfo, securityCheckInstanceCreator.apply(instance));
                        }
                    }
                } else {
                    throw new IllegalStateException(
                            "Class " + target.asClass() + " is annotated with multiple security annotations " + instance.name()
                                    + " and " + existingClassInstance.name());
                }
            }

        }

        return result;
    }

    /**
     * Determine the classes that make up the provider and its services
     *
     * @param providerName - JCA provider name
     * @return class names that make up the provider and its services
     */
    private List<String> registerProvider(String providerName) {
        ArrayList<String> providerClasses = new ArrayList<>();
        Provider provider = Security.getProvider(providerName);
        providerClasses.add(provider.getClass().getName());
        Set<Provider.Service> services = provider.getServices();
        for (Provider.Service service : services) {
            String serviceClass = service.getClassName();
            providerClasses.add(serviceClass);
            // Need to pull in the key classes
            String supportedKeyClasses = service.getAttribute("SupportedKeyClasses");
            if (supportedKeyClasses != null) {
                String[] keyClasses = supportedKeyClasses.split("\\|");
                providerClasses.addAll(Arrays.asList(keyClasses));
            }
        }
        return providerClasses;
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.SECURITY);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityAssociation.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(IdentityProviderManagerCreator.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityProxy.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(X509IdentityProvider.class));
    }
}
