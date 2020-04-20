package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityCheckInstantiationUtil.authenticatedSecurityCheck;
import static io.quarkus.security.deployment.SecurityCheckInstantiationUtil.denyAllSecurityCheck;
import static io.quarkus.security.deployment.SecurityCheckInstantiationUtil.permitAllSecurityCheck;
import static io.quarkus.security.deployment.SecurityCheckInstantiationUtil.rolesAllowedSecurityCheck;

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
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanRegistrarBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.runtime.IdentityProviderManagerCreator;
import io.quarkus.security.runtime.SecurityBuildTimeConfig;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.runtime.SecurityIdentityProxy;
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
    void gatherSecurityChecks(BuildProducer<BeanRegistrarBuildItem> beanRegistrars,
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            BuildProducer<ApplicationClassPredicateBuildItem> classPredicate,
            List<AdditionalSecuredClassesBuildIem> additionalSecuredClasses,
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

        beanRegistrars.produce(new BeanRegistrarBuildItem(new BeanRegistrar() {

            @Override
            public void register(RegistrationContext registrationContext) {
                IndexView index = beanArchiveBuildItem.getIndex();
                Map<MethodInfo, Function<BytecodeCreator, ResultHandle>> securityChecks = gatherSecurityAnnotations(
                        index, additionalSecured, config.denyUnannotated);
                for (AdditionalSecurityCheckBuildItem additionalSecurityCheck : additionalSecurityChecks) {
                    securityChecks.put(additionalSecurityCheck.getMethodInfo(),
                            additionalSecurityCheck.getSecurityCheckResultHandleCreator());
                }

                DotName name = DotName.createSimple(SecurityCheckStorage.class.getName());

                BeanConfigurator<Object> configurator = registrationContext.configure(name);
                configurator.addType(name);
                configurator.scope(BuiltinScope.APPLICATION.getInfo());
                configurator.creator(creator -> {
                    ResultHandle storageBuilder = creator
                            .newInstance(MethodDescriptor.ofConstructor(SecurityCheckStorageBuilder.class));
                    for (Map.Entry<MethodInfo, Function<BytecodeCreator, ResultHandle>> methodEntry : securityChecks
                            .entrySet()) {
                        registerSecuredMethod(storageBuilder, creator, methodEntry);
                    }
                    ResultHandle ret = creator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(SecurityCheckStorageBuilder.class, "create",
                                    SecurityCheckStorage.class),
                            storageBuilder);
                    creator.returnValue(ret);
                });
                configurator.done();
            }
        }));
    }

    private void registerSecuredMethod(ResultHandle checkStorage,
            MethodCreator methodCreator,
            Map.Entry<MethodInfo, Function<BytecodeCreator, ResultHandle>> methodEntry) {
        MethodInfo methodInfo = methodEntry.getKey();
        ResultHandle declaringClass = methodCreator.load(methodInfo.declaringClass().name().toString());
        ResultHandle methodName = methodCreator.load(methodInfo.name());
        ResultHandle methodParamTypes = paramTypes(methodCreator, methodInfo.parameters());

        methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(SecurityCheckStorageBuilder.class, "registerCheck", void.class, String.class,
                        String.class, String[].class, SecurityCheck.class),
                checkStorage,
                declaringClass, methodName, methodParamTypes, methodEntry.getValue().apply(methodCreator));
    }

    private ResultHandle paramTypes(MethodCreator ctor, List<Type> parameters) {
        ResultHandle result = ctor.newArray(String.class, parameters.size());

        for (int i = 0; i < parameters.size(); i++) {
            ctor.writeArrayValue(result, i, ctor.load(parameters.get(i).name().toString()));
        }

        return result;
    }

    private Map<MethodInfo, Function<BytecodeCreator, ResultHandle>> gatherSecurityAnnotations(
            IndexView index,
            Map<DotName, ClassInfo> additionalSecuredClasses, boolean denyUnannotated) {

        Map<MethodInfo, AnnotationInstance> methodToInstanceCollector = new HashMap<>();
        Map<MethodInfo, Function<BytecodeCreator, ResultHandle>> result = new HashMap<>(gatherSecurityAnnotations(
                index, DotNames.ROLES_ALLOWED, methodToInstanceCollector,
                (instance -> rolesAllowedSecurityCheck(instance.value().asStringArray()))));
        result.putAll(gatherSecurityAnnotations(index, DotNames.PERMIT_ALL, methodToInstanceCollector,
                (instance -> permitAllSecurityCheck())));
        result.putAll(gatherSecurityAnnotations(index, DotNames.AUTHENTICATED, methodToInstanceCollector,
                (instance -> authenticatedSecurityCheck())));

        result.putAll(gatherSecurityAnnotations(index, DotNames.DENY_ALL, methodToInstanceCollector,
                (instance -> denyAllSecurityCheck())));

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
                    result.put(methodInfo, denyAllSecurityCheck());
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
                    result.put(methodInfo, denyAllSecurityCheck());
                }
            }
        }

        return result;
    }

    private boolean isPublicNonStaticNonConstructor(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && !"<init>".equals(methodInfo.name());
    }

    private Map<MethodInfo, Function<BytecodeCreator, ResultHandle>> gatherSecurityAnnotations(
            IndexView index, DotName dotName,
            Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods,
            Function<AnnotationInstance, Function<BytecodeCreator, ResultHandle>> securityCheckInstanceCreator) {

        Map<MethodInfo, Function<BytecodeCreator, ResultHandle>> result = new HashMap<>();

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
                for (MethodInfo methodInfo : methods) {
                    AnnotationInstance alreadyExistingInstance = alreadyCheckedMethods.get(methodInfo);
                    if ((alreadyExistingInstance == null)) {
                        result.put(methodInfo, securityCheckInstanceCreator.apply(instance));
                    } else if (alreadyExistingInstance.target().kind() == AnnotationTarget.Kind.CLASS) {
                        throw new IllegalStateException(
                                "Class " + methodInfo.declaringClass() + " is annotated with multiple security annotations");
                    }
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
        return new CapabilityBuildItem(Capabilities.SECURITY);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityAssociation.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(IdentityProviderManagerCreator.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityProxy.class));
    }
}
