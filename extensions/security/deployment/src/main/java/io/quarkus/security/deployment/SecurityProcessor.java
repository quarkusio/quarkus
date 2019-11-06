package io.quarkus.security.deployment;

import java.lang.reflect.Method;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrarBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
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
        if (security.securityProviders != null) {
            for (String providerName : security.securityProviders) {
                jcaProviders.produce(new JCAProviderBuildItem(providerName));
                log.debugf("Added providerName: %s", providerName);
            }
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
    void transformSecurityAnnotations(BuildProducer<AnnotationsTransformerBuildItem> transformers,
            SecurityBuildTimeConfig config) {
        if (config.denyUnannotated) {
            transformers.produce(new AnnotationsTransformerBuildItem(new DenyingUnannotatedTransformer()));
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

    @BuildStep
    void gatherSecurityChecks(BuildProducer<BeanRegistrarBuildItem> beanRegistrars,
            ApplicationIndexBuildItem indexBuildItem,
            BuildProducer<ApplicationClassPredicateBuildItem> classPredicate) {
        classPredicate.produce(new ApplicationClassPredicateBuildItem(new SecurityCheckStorage.AppPredicate()));

        beanRegistrars.produce(new BeanRegistrarBuildItem(new BeanRegistrar() {

            @Override
            public void register(RegistrationContext registrationContext) {
                Map<MethodInfo, AnnotationInstance> methodAnnotations = gatherSecurityAnnotations(indexBuildItem,
                        registrationContext);

                DotName name = DotName.createSimple(SecurityCheckStorage.class.getName());

                BeanConfigurator<Object> configurator = registrationContext.configure(name);
                configurator.addType(name);
                configurator.scope(BuiltinScope.APPLICATION.getInfo());
                configurator.creator(m -> {
                    ResultHandle storageBuilder = m
                            .newInstance(MethodDescriptor.ofConstructor(SecurityCheckStorageBuilder.class));
                    for (Map.Entry<MethodInfo, AnnotationInstance> methodEntry : methodAnnotations.entrySet()) {
                        registerSecuredMethod(storageBuilder, m, methodEntry);
                    }
                    ResultHandle ret = m.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(SecurityCheckStorageBuilder.class, "create",
                                    SecurityCheckStorage.class),
                            storageBuilder);
                    m.returnValue(ret);
                });
                configurator.done();
            }
        }));
    }

    private void registerSecuredMethod(ResultHandle checkStorage,
            MethodCreator methodCreator,
            Map.Entry<MethodInfo, AnnotationInstance> methodEntry) {
        try {
            MethodInfo method = methodEntry.getKey();
            ResultHandle aClass = methodCreator.load(method.declaringClass().name().toString());
            ResultHandle methodName = methodCreator.load(method.name());
            ResultHandle params = paramTypes(methodCreator, method.parameters());

            AnnotationInstance instance = methodEntry.getValue();
            ResultHandle securityAnnotation = methodCreator.load(instance.name().toString());

            ResultHandle annotationParameters = annotationValues(methodCreator, instance);

            Method registerAnnotation = SecurityCheckStorageBuilder.class.getDeclaredMethod("registerAnnotation",
                    String.class, String.class, String[].class, String.class, String[].class);
            methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(registerAnnotation), checkStorage,
                    aClass, methodName, params, securityAnnotation, annotationParameters);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("registerAnnotation method not found on on SecurityCheckStorage", e);
        }
    }

    private ResultHandle annotationValues(MethodCreator methodCreator, AnnotationInstance instance) {
        AnnotationValue value = instance.value();
        if (value != null && value.asStringArray() != null) {
            String[] values = value.asStringArray();
            ResultHandle result = methodCreator.newArray(String.class, methodCreator.load(values.length));
            int i = 0;
            for (String val : values) {
                methodCreator.writeArrayValue(result, i, methodCreator.load(val));
            }
            return result;
        }
        return methodCreator.loadNull();
    }

    private ResultHandle paramTypes(MethodCreator ctor, List<Type> parameters) {
        ResultHandle result = ctor.newArray(String.class, ctor.load(parameters.size()));

        for (int i = 0; i < parameters.size(); i++) {
            ctor.writeArrayValue(result, i, ctor.load(parameters.get(i).toString()));
        }

        return result;
    }

    private Map<MethodInfo, AnnotationInstance> gatherSecurityAnnotations(ApplicationIndexBuildItem indexBuildItem,
            BeanRegistrar.RegistrationContext registrationContext) {
        Set<DotName> securityAnnotations = SecurityAnnotationsRegistrar.SECURITY_BINDINGS.keySet();
        AnnotationStore annotationStore = registrationContext.get(BuildExtension.Key.ANNOTATION_STORE);
        Set<ClassInfo> classesWithSecurity = new HashSet<>();

        Collection<ClassInfo> classes = indexBuildItem.getIndex().getKnownClasses();
        for (ClassInfo classInfo : classes) {
            boolean hasSecurityAnnotations = annotationStore.hasAnyAnnotation(classInfo, securityAnnotations);
            if (!hasSecurityAnnotations) {
                for (MethodInfo method : classInfo.methods()) {
                    if (annotationStore.hasAnyAnnotation(method, securityAnnotations)) {
                        hasSecurityAnnotations = true;
                        break;
                    }
                }
            }
            if (hasSecurityAnnotations) {
                classesWithSecurity.add(classInfo);
            }
        }

        return gatherSecurityAnnotations(securityAnnotations,
                classesWithSecurity, annotationStore);
    }

    private Map<MethodInfo, AnnotationInstance> gatherSecurityAnnotations(Set<DotName> securityAnnotations,
            Set<ClassInfo> classesWithSecurity,
            AnnotationStore annotationStore) {
        Map<MethodInfo, AnnotationInstance> methodAnnotations = new HashMap<>();
        for (ClassInfo classInfo : classesWithSecurity) {
            Collection<AnnotationInstance> classAnnotations = annotationStore.getAnnotations(classInfo);
            AnnotationInstance classLevelAnnotation = getSingle(classAnnotations, securityAnnotations);

            for (MethodInfo method : classInfo.methods()) {
                AnnotationInstance methodAnnotation = getSingle(annotationStore.getAnnotations(method), securityAnnotations);
                methodAnnotation = methodAnnotation == null ? classLevelAnnotation : methodAnnotation;
                if (methodAnnotation != null) {
                    methodAnnotations.put(method, methodAnnotation);
                }
            }
        }
        return methodAnnotations;
    }

    private AnnotationInstance getSingle(Collection<AnnotationInstance> classAnnotations, Set<DotName> securityAnnotations) {
        AnnotationInstance result = null;
        for (AnnotationInstance annotation : classAnnotations) {
            if (securityAnnotations.contains(annotation.name())) {
                if (result != null) {
                    throw new IllegalStateException("Duplicate security annotations on class " + annotation.target());
                }
                result = annotation;
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

    @BuildStep(providesCapabilities = Capabilities.SECURITY)
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
