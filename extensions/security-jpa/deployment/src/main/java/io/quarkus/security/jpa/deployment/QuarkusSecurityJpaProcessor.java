package io.quarkus.security.jpa.deployment;

import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildIdentity;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildTrustedIdentity;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.Session;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
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

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JPA);
    }

    @BuildStep
    void configureJpaAuthConfig(ApplicationIndexBuildItem index,
            BuildProducer<GeneratedBeanBuildItem> beanProducer,
            Optional<JpaSecurityDefinitionBuildItem> jpaSecurityDefinitionBuildItem,
            PanacheEntityPredicateBuildItem panacheEntityPredicate) {

        if (jpaSecurityDefinitionBuildItem.isPresent()) {
            JpaSecurityDefinition jpaSecurityDefinition = jpaSecurityDefinitionBuildItem.get().get();

            generateIdentityProvider(index.getIndex(), jpaSecurityDefinition, jpaSecurityDefinition.passwordType(),
                    jpaSecurityDefinition.customPasswordProvider(), beanProducer, panacheEntityPredicate);

            generateTrustedIdentityProvider(index.getIndex(), jpaSecurityDefinition,
                    beanProducer, panacheEntityPredicate);
        }
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
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate) {
        GeneratedBeanGizmoAdaptor gizmoAdaptor = new GeneratedBeanGizmoAdaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaIdentityProviderImpl";
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(name)
                .superClass(JpaIdentityProvider.class)
                .classOutput(gizmoAdaptor)
                .build()) {
            classCreator.addAnnotation(Singleton.class);
            FieldDescriptor passwordProviderField = classCreator.getFieldCreator("passwordProvider", PasswordProvider.class)
                    .setModifiers(Modifier.PRIVATE)
                    .getFieldDescriptor();

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

    private void generateTrustedIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate) {
        GeneratedBeanGizmoAdaptor gizmoAdaptor = new GeneratedBeanGizmoAdaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaTrustedIdentityProviderImpl";
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(name)
                .superClass(JpaTrustedIdentityProvider.class)
                .classOutput(gizmoAdaptor)
                .build()) {
            classCreator.addAnnotation(Singleton.class);
            try (MethodCreator methodCreator = classCreator.getMethodCreator("authenticate", SecurityIdentity.class,
                    EntityManager.class, TrustedAuthenticationRequest.class)) {
                methodCreator.setModifiers(Modifier.PUBLIC);

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

}
