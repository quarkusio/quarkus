package io.quarkus.security.jpa.deployment;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildIdentity;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildTrustedIdentity;

import java.lang.constant.ClassDesc;
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

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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
import io.quarkus.security.jpa.runtime.JpaIdentityProvider;
import io.quarkus.security.jpa.runtime.JpaTrustedIdentityProvider;

class QuarkusSecurityJpaProcessor {

    private static final DotName DOTNAME_NATURAL_ID = DotName.createSimple(NaturalId.class.getName());
    private static final DotName SESSION_FACTORY_FACTORY = DotName.createSimple(SessionFactory.class.getName());
    private static final DotName JPA_IDENTITY_PROVIDER_NAME = DotName.createSimple(JpaIdentityProvider.class.getName());
    private static final DotName JPA_TRUSTED_IDENTITY_PROVIDER_NAME = DotName
            .createSimple(JpaTrustedIdentityProvider.class.getName());
    private static final DotName PERSISTENCE_UNIT_NAME = DotName.createSimple(PersistenceUnit.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JPA);
    }

    @BuildStep
    void configureJpaAuthConfig(ApplicationIndexBuildItem index, List<PersistenceUnitDescriptorBuildItem> puDescriptors,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, SecurityJpaBuildTimeConfig secJpaConfig,
            Optional<JpaSecurityDefinitionBuildItem> jpaSecurityDefinitionBuildItem,
            PanacheEntityPredicateBuildItem panacheEntityPredicate) {

        if (jpaSecurityDefinitionBuildItem.isPresent()) {
            final boolean requireActiveCDIRequestContext = shouldActivateCDIReqCtx(puDescriptors, secJpaConfig);
            JpaSecurityDefinition jpaSecurityDefinition = jpaSecurityDefinitionBuildItem.get().get();

            generateIdentityProvider(index.getIndex(), jpaSecurityDefinition, jpaSecurityDefinition.passwordType(),
                    jpaSecurityDefinition.customPasswordProvider(), beanProducer, panacheEntityPredicate,
                    requireActiveCDIRequestContext);

            generateTrustedIdentityProvider(index.getIndex(), jpaSecurityDefinition,
                    beanProducer, panacheEntityPredicate, requireActiveCDIRequestContext);
        }
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
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate,
            boolean requireActiveCDIRequestContext) {
        GeneratedBeanGizmo2Adaptor gizmoAdaptor = new GeneratedBeanGizmo2Adaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaIdentityProviderImpl";
        Gizmo.create(gizmoAdaptor)
                .class_(name, cc -> {
                    cc.extends_(JpaIdentityProvider.class);
                    cc.addAnnotation(Singleton.class);
                    cc.defaultConstructor();
                    FieldDesc passwordProviderField = cc.field("passwordProvider", ifc -> {
                        ifc.setType(PasswordProvider.class);
                        ifc.private_();
                    });

                    if (requireActiveCDIRequestContext) {
                        activateCDIRequestContext(cc);
                    }

                    cc.method("authenticate", mc -> {
                        mc.public_();
                        mc.returning(SecurityIdentity.class);
                        var emParam = mc.parameter("entityManager", EntityManager.class);
                        var requestParam = mc.parameter("request", UsernamePasswordAuthenticationRequest.class);
                        mc.body(bc -> {
                            LocalVar username = bc.localVar("username", bc.invokeVirtual(
                                    MethodDesc.of(UsernamePasswordAuthenticationRequest.class, "getUsername", String.class),
                                    requestParam));

                            // two strategies, depending on whether the username is natural id
                            AnnotationInstance naturalIdAnnotation = jpaSecurityDefinition.username
                                    .annotation(DOTNAME_NATURAL_ID);
                            Expr user = lookupUserById(jpaSecurityDefinition, name, bc, cc.this_(), emParam, username,
                                    naturalIdAnnotation, JpaIdentityProvider.class);

                            String declaringClassName = jpaSecurityDefinition.annotatedClass.name().toString();
                            LocalVar userVar = bc.localVar("user",
                                    bc.cast(user, ClassDesc.of(declaringClassName)));

                            buildIdentity(index, jpaSecurityDefinition, passwordTypeValue, passwordProviderValue,
                                    panacheEntityPredicate,
                                    passwordProviderField, cc.this_(), requestParam, userVar, bc);
                        });
                    });
                });
    }

    private void generateTrustedIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate,
            boolean requireActiveCDIRequestContext) {
        GeneratedBeanGizmo2Adaptor gizmoAdaptor = new GeneratedBeanGizmo2Adaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaTrustedIdentityProviderImpl";
        Gizmo.create(gizmoAdaptor).class_(name, cc -> {
            cc.extends_(JpaTrustedIdentityProvider.class);
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();

            if (requireActiveCDIRequestContext) {
                activateCDIRequestContext(cc);
            }

            cc.method("authenticate", mc -> {
                mc.public_();
                mc.returning(SecurityIdentity.class);
                var emParam = mc.parameter("entityManager", EntityManager.class);
                var requestParam = mc.parameter("request", TrustedAuthenticationRequest.class);
                mc.body(bc -> {
                    LocalVar username = bc.localVar("username", bc.invokeVirtual(
                            MethodDesc.of(TrustedAuthenticationRequest.class, "getPrincipal", String.class),
                            requestParam));

                    // two strategies, depending on whether the username is natural id
                    AnnotationInstance naturalIdAnnotation = jpaSecurityDefinition.username.annotation(DOTNAME_NATURAL_ID);
                    Expr user = lookupUserById(jpaSecurityDefinition, name, bc, cc.this_(), emParam, username,
                            naturalIdAnnotation, JpaTrustedIdentityProvider.class);

                    String declaringClassName = jpaSecurityDefinition.annotatedClass.name().toString();
                    LocalVar userVar = bc.localVar("user",
                            bc.cast(user, ClassDesc.of(declaringClassName)));

                    buildTrustedIdentity(index, jpaSecurityDefinition, panacheEntityPredicate, requestParam,
                            userVar, bc);
                });
            });
        });
    }

    private Expr lookupUserById(JpaSecurityDefinition jpaSecurityDefinition, String name, BlockCreator bc,
            Expr thisRef, Expr emParam, Expr username, AnnotationInstance naturalIdAnnotation,
            Class<?> identityProviderClass) {
        if (naturalIdAnnotation != null) {
            // Session session = em.unwrap(Session.class);
            Expr session = bc.invokeInterface(
                    MethodDesc.of(EntityManager.class, "unwrap", Object.class, Class.class),
                    emParam,
                    bc.classForName(Const.of(Session.class.getName())));
            // SimpleNaturalIdLoadAccess<PlainUserEntity> naturalIdLoadAccess = session.bySimpleNaturalId(PlainUserEntity.class);
            Expr naturalIdLoadAccess = bc.invokeInterface(
                    MethodDesc.of(Session.class, "bySimpleNaturalId",
                            SimpleNaturalIdLoadAccess.class, Class.class),
                    bc.cast(session, Session.class),
                    bc.classForName(Const.of(jpaSecurityDefinition.annotatedClass.name().toString())));
            // PlainUserEntity user = naturalIdLoadAccess.load(request.getUsername());
            return bc.invokeInterface(
                    MethodDesc.of(SimpleNaturalIdLoadAccess.class, "load",
                            Object.class, Object.class),
                    naturalIdLoadAccess, username);
        } else {
            // Query query = entityManager.createQuery("FROM Entity WHERE field = :name")
            String hql = "FROM " + jpaSecurityDefinition.annotatedClass.simpleName() + " WHERE "
                    + jpaSecurityDefinition.username.name() + " = :name";
            Expr query = bc.invokeInterface(
                    MethodDesc.of(EntityManager.class, "createQuery", Query.class, String.class),
                    emParam, Const.of(hql));
            // query.setParameter("name", request.getUsername())
            Expr query2 = bc.invokeInterface(
                    MethodDesc.of(Query.class, "setParameter", Query.class, String.class, Object.class),
                    query, Const.of("name"), username);

            // UserEntity user = getSingleUser(query2);
            return bc.invokeVirtual(
                    MethodDesc.of(identityProviderClass, "getSingleUser", Object.class, Query.class),
                    thisRef, query2);
        }
    }

    private static void activateCDIRequestContext(ClassCreator cc) {
        cc.method("requireActiveCDIRequestContext", mc -> {
            mc.protected_();
            mc.returning(boolean.class);
            mc.body(bc -> {
                bc.return_(true);
            });
        });
    }

    private static boolean shouldActivateCDIReqCtx(List<PersistenceUnitDescriptorBuildItem> puDescriptors,
            SecurityJpaBuildTimeConfig secJpaConfig) {
        var descriptor = puDescriptors.stream()
                .filter(desc -> secJpaConfig.persistenceUnitName().equals(desc.getPersistenceUnitName())).findFirst();
        if (descriptor.isEmpty()) {
            throw new ConfigurationException("Persistence unit '" + secJpaConfig.persistenceUnitName()
                    + "' specified with the 'quarkus.security-jpa.persistence-unit-name' configuration property"
                    + " does not exist. Please set valid persistence unit name.");
        }
        // 'io.quarkus.hibernate.orm.runtime.tenant.TenantResolver' is only resolved when CDI request context is active
        // we need to active request context even when TenantResolver is @ApplicationScoped for tenant to be set
        // see io.quarkus.hibernate.orm.runtime.tenant.HibernateCurrentTenantIdentifierResolver.resolveCurrentTenantIdentifier
        // for more information
        return descriptor.get().getConfig().getMultiTenancyStrategy() != MultiTenancyStrategy.NONE;
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
