package io.quarkus.security.jpa.reactive.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildIdentity;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildTrustedIdentity;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.inject.Singleton;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.reactive.common.Identifier;
import org.hibernate.reactive.mutiny.Mutiny;
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
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.PasswordProvider;
import io.quarkus.security.jpa.common.deployment.JpaSecurityDefinition;
import io.quarkus.security.jpa.common.deployment.JpaSecurityDefinitionBuildItem;
import io.quarkus.security.jpa.common.deployment.PanacheEntityPredicateBuildItem;
import io.quarkus.security.jpa.reactive.runtime.JpaReactiveIdentityProvider;
import io.quarkus.security.jpa.reactive.runtime.JpaReactiveTrustedIdentityProvider;
import io.smallrye.mutiny.Uni;

class QuarkusSecurityJpaReactiveProcessor {

    private static final DotName NATURAL_ID = DotName.createSimple(NaturalId.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JPA_REACTIVE);
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

    private static Set<String> collectPanacheEntities(List<PanacheEntityClassesBuildItem> panacheEntityClassesBuildItems) {
        Set<String> modelClasses = new HashSet<>();
        for (PanacheEntityClassesBuildItem panacheEntityClasses : panacheEntityClassesBuildItems) {
            modelClasses.addAll(panacheEntityClasses.getEntityClasses());
        }
        return modelClasses;
    }

    private static void generateIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            AnnotationValue passwordTypeValue, AnnotationValue passwordProviderValue,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate) {
        GeneratedBeanGizmoAdaptor gizmoAdaptor = new GeneratedBeanGizmoAdaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaReactiveIdentityProviderImpl";
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(name)
                .superClass(JpaReactiveIdentityProvider.class)
                .classOutput(gizmoAdaptor)
                .build()) {
            classCreator.addAnnotation(Singleton.class);
            FieldDescriptor passwordProviderField = classCreator.getFieldCreator("passwordProvider", PasswordProvider.class)
                    .setModifiers(0) // removes default modifier => makes field package-private
                    .getFieldDescriptor();

            MethodDescriptor methodToImpl = MethodDescriptor.ofMethod(JpaReactiveIdentityProvider.class, "authenticate",
                    Uni.class, Mutiny.Session.class, UsernamePasswordAuthenticationRequest.class);
            try (MethodCreator methodCreator = classCreator.getMethodCreator(methodToImpl)) {
                methodCreator.setModifiers(Modifier.PUBLIC);

                ResultHandle username = methodCreator.invokeVirtualMethod(
                        ofMethod(UsernamePasswordAuthenticationRequest.class, "getUsername", String.class),
                        methodCreator.getMethodParam(1));

                ResultHandle userUni = lookupUserById(jpaSecurityDefinition, methodCreator, username);

                // .map(user -> { /* build identity */ })
                ResultHandle identityUni = uniMap(methodCreator, userUni,
                        (body, user) -> buildIdentity(index, jpaSecurityDefinition, passwordTypeValue, passwordProviderValue,
                                panacheEntityPredicate, passwordProviderField, methodCreator, user, body));

                methodCreator.returnValue(identityUni);
            }
        }
    }

    private static void generateTrustedIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate) {
        GeneratedBeanGizmoAdaptor gizmoAdaptor = new GeneratedBeanGizmoAdaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaReactiveTrustedIdentityProviderImpl";
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(name)
                .superClass(JpaReactiveTrustedIdentityProvider.class)
                .classOutput(gizmoAdaptor)
                .build()) {
            classCreator.addAnnotation(Singleton.class);

            MethodDescriptor methodToImpl = MethodDescriptor.ofMethod(JpaReactiveTrustedIdentityProvider.class, "authenticate",
                    Uni.class, Mutiny.Session.class, TrustedAuthenticationRequest.class);
            try (MethodCreator methodCreator = classCreator.getMethodCreator(methodToImpl)) {
                methodCreator.setModifiers(Modifier.PUBLIC);

                ResultHandle username = methodCreator.invokeVirtualMethod(
                        ofMethod(TrustedAuthenticationRequest.class, "getPrincipal", String.class),
                        methodCreator.getMethodParam(1));

                ResultHandle userUni = lookupUserById(jpaSecurityDefinition, methodCreator, username);

                // .map(user -> { /* build identity */ })
                ResultHandle identityUni = uniMap(methodCreator, userUni,
                        (body, user) -> buildTrustedIdentity(index, jpaSecurityDefinition, panacheEntityPredicate,
                                methodCreator, user, body));

                methodCreator.returnValue(identityUni);
            }
        }
    }

    private static ResultHandle lookupUserById(JpaSecurityDefinition jpaSecurityDefinition, MethodCreator methodCreator,
            ResultHandle username) {

        // two strategies, depending on whether the username is natural id
        AnnotationInstance naturalIdAnnotation = jpaSecurityDefinition.username.annotation(NATURAL_ID);

        // PlainUserEntity.class
        String userEntityClassName = jpaSecurityDefinition.annotatedClass.name().toString();
        var userEntityClass = methodCreator.loadClass(userEntityClassName);

        // 'Mutiny.Session' session
        ResultHandle session = methodCreator.getMethodParam(0);

        boolean fetchJoinRoles = shouldFetchJoinRoles(jpaSecurityDefinition);
        ResultHandle user;
        if (naturalIdAnnotation != null) {

            // Identifier.id("name", username)
            ResultHandle id = methodCreator.invokeStaticMethod(
                    ofMethod(Identifier.class, "id", Identifier.Id.class, String.class, Object.class),
                    methodCreator.load(jpaSecurityDefinition.username.name()), username);

            // session.find(PlainUserEntity.class, Identifier.id("name", username))
            user = methodCreator.invokeInterfaceMethod(
                    ofMethod(Mutiny.Session.class, "find", Uni.class, Class.class, Identifier.class),
                    session, userEntityClass, id);

            if (fetchJoinRoles) {
                // .flatMap(user -> session.fetch(user))
                String userClassName = jpaSecurityDefinition.annotatedClass.name().toString();
                user = uniFlatMap(methodCreator, user, (body, user1) -> body.invokeInterfaceMethod(
                        ofMethod(Mutiny.Session.class, "fetch", Uni.class, userClassName),
                        session, user1));
            }
        } else {

            final String hql;
            if (fetchJoinRoles) {
                // "FROM Entity en LEFT JOIN FETCH en.rolesField WHERE en.field = :name"
                hql = "FROM " + jpaSecurityDefinition.annotatedClass.simpleName() + " en LEFT JOIN FETCH en."
                        + jpaSecurityDefinition.roles.name() + " WHERE en."
                        + jpaSecurityDefinition.username.name() + " = :name";
            } else {
                // "FROM Entity WHERE field = :name"
                hql = "FROM " + jpaSecurityDefinition.annotatedClass.simpleName() + " WHERE "
                        + jpaSecurityDefinition.username.name() + " = :name";
            }

            // session.createQuery("<<HQL>>", UserEntity.class)
            ResultHandle query1 = methodCreator.invokeInterfaceMethod(
                    ofMethod(Mutiny.Session.class, "createQuery", Mutiny.SelectionQuery.class, String.class, Class.class),
                    session, methodCreator.load(hql), userEntityClass);

            // .setParameter("name", username)
            ResultHandle query2 = methodCreator.invokeInterfaceMethod(
                    ofMethod(Mutiny.SelectionQuery.class, "setParameter", Mutiny.SelectionQuery.class, String.class,
                            Object.class),
                    query1, methodCreator.load("name"), username);

            // .getSingleResultOrNull()
            user = methodCreator.invokeInterfaceMethod(
                    ofMethod(Mutiny.SelectionQuery.class, "getSingleResultOrNull", Uni.class),
                    query2);
        }

        return user;
    }

    private static boolean shouldFetchJoinRoles(JpaSecurityDefinition jpaSecurityDefinition) {
        return jpaSecurityDefinition.haveRolesAnnotation(
                DotName.createSimple(CollectionTable.class),
                DotName.createSimple(ManyToMany.class),
                DotName.createSimple(OneToMany.class),
                DotName.createSimple(ManyToOne.class));
    }

    private static ResultHandle uniMap(MethodCreator creator, ResultHandle uniInstance,
            BiConsumer<BytecodeCreator, ResultHandle> fun) {
        return uniLambda(creator, uniInstance, fun, "map");
    }

    private static ResultHandle uniFlatMap(BytecodeCreator creator, ResultHandle uniInstance,
            BiConsumer<BytecodeCreator, ResultHandle> fun) {
        return uniLambda(creator, uniInstance, fun, "flatMap");
    }

    private static ResultHandle uniLambda(BytecodeCreator creator, ResultHandle uniInstance,
            BiConsumer<BytecodeCreator, ResultHandle> function, String name) {
        FunctionCreator lambda = creator.createFunction(Function.class);
        BytecodeCreator body = lambda.getBytecode();
        ResultHandle user = body.getMethodParam(0);
        function.accept(body, user);

        return creator.invokeInterfaceMethod(ofMethod(Uni.class, name, Uni.class, Function.class),
                uniInstance, lambda.getInstance());
    }
}
