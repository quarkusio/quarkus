package io.quarkus.security.jpa.deployment;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildIdentity;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildTrustedIdentity;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.PasswordProvider;
import io.quarkus.security.jpa.common.deployment.JpaSecurityDefinition;
import io.quarkus.security.jpa.common.deployment.JpaSecurityDefinitionBuildItem;
import io.quarkus.security.jpa.common.deployment.PanacheEntityPredicateBuildItem;
import io.quarkus.security.jpa.common.deployment.SecurityJpaProviderInfoBuildItem;
import io.quarkus.security.jpa.runtime.JpaIdentityProvider;
import io.quarkus.security.jpa.runtime.JpaTrustedIdentityProvider;
import io.quarkus.security.jpa.runtime.SecurityJpaProvider;

class QuarkusSecurityJpaProcessor {

    private static final DotName DOTNAME_NATURAL_ID = DotName.createSimple(NaturalId.class.getName());
    private static final DotName SESSION_FACTORY_FACTORY = DotName.createSimple(SessionFactory.class.getName());
    private static final DotName JPA_IDENTITY_PROVIDER_NAME = DotName.createSimple(JpaIdentityProvider.class.getName());
    private static final DotName JPA_TRUSTED_IDENTITY_PROVIDER_NAME = DotName
            .createSimple(JpaTrustedIdentityProvider.class.getName());
    private static final DotName PERSISTENCE_UNIT_NAME = DotName.createSimple(PersistenceUnit.class.getName());
    private static final Logger LOGGER = Logger.getLogger(QuarkusSecurityJpaProcessor.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JPA);
    }

    @BuildStep
    void registerSecurityJpaProviderClass(Optional<JpaSecurityDefinitionBuildItem> jpaSecurityDefinitionBuildItem,
            BuildProducer<SecurityJpaProviderInfoBuildItem> securityJpaProviderClassProducer) {
        if (jpaSecurityDefinitionBuildItem.isPresent()) {
            var definition = jpaSecurityDefinitionBuildItem.get().get();
            securityJpaProviderClassProducer.produce(new SecurityJpaProviderInfoBuildItem(SecurityJpaProvider.class,
                    getJpaIdentityProviderClassName(definition), getTrustedIdentityProviderName(definition),
                    JpaIdentityProvider.class, JpaTrustedIdentityProvider.class));
        }
    }

    @BuildStep
    void configureJpaAuthConfig(ApplicationIndexBuildItem index, List<PersistenceUnitDescriptorBuildItem> puDescriptors,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, SecurityJpaBuildTimeConfig secJpaConfig,
            Optional<JpaSecurityDefinitionBuildItem> jpaSecurityDefinitionBuildItem,
            PanacheEntityPredicateBuildItem panacheEntityPredicate, BuildProducer<GeneratedClassBuildItem> classProducer) {

        if (jpaSecurityDefinitionBuildItem.isPresent()) {
            var descriptor = findPersistenceUnitDescriptor(secJpaConfig, puDescriptors);
            final boolean registerProviderAsCdiBean;
            if (descriptor == null) {
                LOGGER.debug("Not generating identity provider CDI beans as the default persistence unit is not available."
                        + " Please either configure the 'quarkus.security-jpa.persistence-unit-name' configuration property"
                        + " or use programmatic API to select correct persistence unit name");
                registerProviderAsCdiBean = false;
            } else {
                registerProviderAsCdiBean = true;
            }

            final boolean requireActiveCDIRequestContext = shouldActivateCDIReqCtx(descriptor, secJpaConfig);
            JpaSecurityDefinition jpaSecurityDefinition = jpaSecurityDefinitionBuildItem.get().get();

            generateIdentityProvider(index.getIndex(), jpaSecurityDefinition, jpaSecurityDefinition.passwordType(),
                    jpaSecurityDefinition.customPasswordProvider(),
                    createClassOutput(beanProducer, classProducer, registerProviderAsCdiBean), panacheEntityPredicate,
                    requireActiveCDIRequestContext, registerProviderAsCdiBean);

            generateTrustedIdentityProvider(index.getIndex(), jpaSecurityDefinition,
                    createClassOutput(beanProducer, classProducer, registerProviderAsCdiBean), panacheEntityPredicate,
                    requireActiveCDIRequestContext, registerProviderAsCdiBean);

        }
    }

    private static ClassOutput createClassOutput(BuildProducer<GeneratedBeanBuildItem> beanProducer,
            BuildProducer<GeneratedClassBuildItem> classProducer, boolean registerProviderAsCdiBean) {
        return registerProviderAsCdiBean ? new GeneratedBeanGizmoAdaptor(beanProducer)
                : new GeneratedClassGizmoAdaptor(classProducer, true);
    }

    @BuildStep(onlyIf = EnabledIfNonDefaultPersistenceUnit.class)
    InjectionPointTransformerBuildItem transformer(SecurityJpaBuildTimeConfig config) {
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {

            @Override
            public boolean appliesTo(Type requiredType) {
                return requiredType.name().equals(SESSION_FACTORY_FACTORY);
            }

            public void transform(TransformationContext context) {
                if (context.getAnnotationTarget().kind() == AnnotationTarget.Kind.FIELD) {
                    var declaringClassName = context.getAnnotationTarget().asField().declaringClass().name();
                    if (JPA_IDENTITY_PROVIDER_NAME.equals(declaringClassName)
                            || JPA_TRUSTED_IDENTITY_PROVIDER_NAME.equals(declaringClassName)) {
                        context.transform()
                                .add(PERSISTENCE_UNIT_NAME,
                                        AnnotationValue.createStringValue("value", config.persistenceUnitName()))
                                .done();
                    }
                }
            }
        });
    }

    @BuildStep
    PanacheEntityPredicateBuildItem panacheEntityPredicate(List<PanacheEntityClassesBuildItem> panacheEntityClasses) {
        return new PanacheEntityPredicateBuildItem(collectPanacheEntities(panacheEntityClasses));
    }

    private Set<String> collectPanacheEntities(List<PanacheEntityClassesBuildItem> panacheEntityClassesBuildItems) {
        Set<String> modelClasses = new HashSet<>();
        for (PanacheEntityClassesBuildItem panacheEntityClasses : panacheEntityClassesBuildItems) {
            modelClasses.addAll(panacheEntityClasses.getEntityClasses());
        }
        return modelClasses;
    }

    private void generateIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            AnnotationValue passwordTypeValue, AnnotationValue passwordProviderValue,
            ClassOutput classOutput, PanacheEntityPredicateBuildItem panacheEntityPredicate,
            boolean requireActiveCDIRequestContext, boolean registerProviderAsCdiBean) {

        String name = getJpaIdentityProviderClassName(jpaSecurityDefinition);
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(name)
                .superClass(JpaIdentityProvider.class)
                .classOutput(classOutput)
                .build()) {

            if (registerProviderAsCdiBean) {
                classCreator.addAnnotation(Singleton.class);
            }

            FieldDescriptor passwordProviderField = classCreator.getFieldCreator("passwordProvider", PasswordProvider.class)
                    .setModifiers(Modifier.PRIVATE)
                    .getFieldDescriptor();

            if (requireActiveCDIRequestContext) {
                activateCDIRequestContext(classCreator);
            }

            try (MethodCreator methodCreator = classCreator.getMethodCreator("authenticate", SecurityIdentity.class,
                    EntityManager.class, UsernamePasswordAuthenticationRequest.class)) {
                methodCreator.setModifiers(Modifier.PUBLIC);

                ResultHandle username = methodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(UsernamePasswordAuthenticationRequest.class, "getUsername", String.class),
                        methodCreator.getMethodParam(1));

                // two strategies, depending on whether the username is natural id
                AnnotationInstance naturalIdAnnotation = jpaSecurityDefinition.username.annotation(DOTNAME_NATURAL_ID);
                ResultHandle user = lookupUserById(jpaSecurityDefinition, name, methodCreator, username, naturalIdAnnotation);

                String declaringClassName = jpaSecurityDefinition.annotatedClass.name().toString();
                String declaringClassTypeDescriptor = "L" + declaringClassName.replace('.', '/') + ";";
                AssignableResultHandle userVar = methodCreator.createVariable(declaringClassTypeDescriptor);
                methodCreator.assign(userVar, methodCreator.checkCast(user, declaringClassName));

                buildIdentity(index, jpaSecurityDefinition, passwordTypeValue, passwordProviderValue, panacheEntityPredicate,
                        passwordProviderField, methodCreator, userVar, methodCreator);
            }
        }
    }

    private static String getJpaIdentityProviderClassName(JpaSecurityDefinition jpaSecurityDefinition) {
        return jpaSecurityDefinition.annotatedClass.name() + "__JpaIdentityProviderImpl";
    }

    private void generateTrustedIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            ClassOutput classOutput, PanacheEntityPredicateBuildItem panacheEntityPredicate,
            boolean requireActiveCDIRequestContext, boolean registerProviderAsCdiBean) {

        String name = getTrustedIdentityProviderName(jpaSecurityDefinition);
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(name)
                .superClass(JpaTrustedIdentityProvider.class)
                .classOutput(classOutput)
                .build()) {

            if (registerProviderAsCdiBean) {
                classCreator.addAnnotation(Singleton.class);
            }

            try (MethodCreator methodCreator = classCreator.getMethodCreator("authenticate", SecurityIdentity.class,
                    EntityManager.class, TrustedAuthenticationRequest.class)) {
                methodCreator.setModifiers(Modifier.PUBLIC);

                if (requireActiveCDIRequestContext) {
                    activateCDIRequestContext(classCreator);
                }

                ResultHandle username = methodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(TrustedAuthenticationRequest.class, "getPrincipal", String.class),
                        methodCreator.getMethodParam(1));

                // two strategies, depending on whether the username is natural id
                AnnotationInstance naturalIdAnnotation = jpaSecurityDefinition.username.annotation(DOTNAME_NATURAL_ID);
                ResultHandle user = lookupUserById(jpaSecurityDefinition, name, methodCreator, username, naturalIdAnnotation);

                String declaringClassName = jpaSecurityDefinition.annotatedClass.name().toString();
                String declaringClassTypeDescriptor = "L" + declaringClassName.replace('.', '/') + ";";
                AssignableResultHandle userVar = methodCreator.createVariable(declaringClassTypeDescriptor);
                methodCreator.assign(userVar, methodCreator.checkCast(user, declaringClassName));

                buildTrustedIdentity(index, jpaSecurityDefinition, panacheEntityPredicate, methodCreator, userVar,
                        methodCreator);
            }
        }
    }

    private static String getTrustedIdentityProviderName(JpaSecurityDefinition jpaSecurityDefinition) {
        return jpaSecurityDefinition.annotatedClass.name() + "__JpaTrustedIdentityProviderImpl";
    }

    private ResultHandle lookupUserById(JpaSecurityDefinition jpaSecurityDefinition, String name, MethodCreator methodCreator,
            ResultHandle username, AnnotationInstance naturalIdAnnotation) {
        ResultHandle user;
        if (naturalIdAnnotation != null) {
            // Session session = em.unwrap(Session.class);
            ResultHandle session = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(EntityManager.class, "unwrap", Object.class, Class.class),
                    methodCreator.getMethodParam(0),
                    methodCreator.loadClassFromTCCL(Session.class));
            // SimpleNaturalIdLoadAccess<PlainUserEntity> naturalIdLoadAccess = session.bySimpleNaturalId(PlainUserEntity.class);
            ResultHandle naturalIdLoadAccess = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Session.class, "bySimpleNaturalId",
                            SimpleNaturalIdLoadAccess.class, Class.class),
                    methodCreator.checkCast(session, Session.class),
                    methodCreator.loadClassFromTCCL(jpaSecurityDefinition.annotatedClass.name().toString()));
            // PlainUserEntity user = naturalIdLoadAccess.load(request.getUsername());
            user = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(SimpleNaturalIdLoadAccess.class, "load",
                            Object.class, Object.class),
                    naturalIdLoadAccess, username);
        } else {
            // Query query = entityManager.createQuery("FROM Entity WHERE field = :name")
            String hql = "FROM " + jpaSecurityDefinition.annotatedClass.simpleName() + " WHERE "
                    + jpaSecurityDefinition.username.name() + " = :name";
            ResultHandle query = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(EntityManager.class, "createQuery", Query.class, String.class),
                    methodCreator.getMethodParam(0), methodCreator.load(hql));
            // query.setParameter("name", request.getUsername())
            ResultHandle query2 = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Query.class, "setParameter", Query.class, String.class, Object.class),
                    query, methodCreator.load("name"), username);

            // UserEntity user = getSingleUser(query2);
            user = methodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(name, "getSingleUser", Object.class, Query.class),
                    methodCreator.getThis(), query2);
        }
        return user;
    }

    private static void activateCDIRequestContext(ClassCreator classCreator) {
        try (MethodCreator methodCreator = classCreator.getMethodCreator("requireActiveCDIRequestContext",
                DotName.createSimple(boolean.class.getName()).toString())) {
            methodCreator.setModifiers(Modifier.PROTECTED);
            methodCreator.returnBoolean(true);
        }
    }

    private static boolean shouldActivateCDIReqCtx(PersistenceUnitDescriptorBuildItem descriptor,
            SecurityJpaBuildTimeConfig secJpaConfig) {
        if (descriptor == null) {
            // we cannot determine correct value for the programmatic setup before hand
            return true;
        }
        // 'io.quarkus.hibernate.orm.runtime.tenant.TenantResolver' is only resolved when CDI request context is active
        // we need to active request context even when TenantResolver is @ApplicationScoped for tenant to be set
        // see io.quarkus.hibernate.orm.runtime.tenant.HibernateCurrentTenantIdentifierResolver.resolveCurrentTenantIdentifier
        // for more information
        return descriptor.getConfig().getMultiTenancyStrategy() != MultiTenancyStrategy.NONE;
    }

    private static PersistenceUnitDescriptorBuildItem findPersistenceUnitDescriptor(SecurityJpaBuildTimeConfig secJpaConfig,
            List<PersistenceUnitDescriptorBuildItem> puDescriptors) {
        var descriptor = puDescriptors.stream()
                .filter(desc -> secJpaConfig.persistenceUnitName().equals(desc.getPersistenceUnitName()))
                .findFirst()
                .orElse(null);
        if (descriptor == null && !DEFAULT_PERSISTENCE_UNIT_NAME.equals(secJpaConfig.persistenceUnitName())) {
            throw new ConfigurationException("Persistence unit '" + secJpaConfig.persistenceUnitName()
                    + "' specified with the 'quarkus.security-jpa.persistence-unit-name' configuration property"
                    + " does not exist. Please set valid persistence unit name.");
        }
        return descriptor;
    }

    static final class EnabledIfNonDefaultPersistenceUnit implements BooleanSupplier {

        private final boolean useNonDefaultPersistenceUnit;

        public EnabledIfNonDefaultPersistenceUnit(SecurityJpaBuildTimeConfig config) {
            this.useNonDefaultPersistenceUnit = !DEFAULT_PERSISTENCE_UNIT_NAME.equals(config.persistenceUnitName());
        }

        @Override
        public boolean getAsBoolean() {
            return useNonDefaultPersistenceUnit;
        }
    }

}
