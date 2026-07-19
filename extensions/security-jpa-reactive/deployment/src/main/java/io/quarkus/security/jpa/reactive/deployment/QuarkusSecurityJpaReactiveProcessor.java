package io.quarkus.security.jpa.reactive.deployment;

import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildIdentity;
import static io.quarkus.security.jpa.common.deployment.JpaSecurityIdentityUtil.buildTrustedIdentity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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
        GeneratedBeanGizmo2Adaptor gizmoAdaptor = new GeneratedBeanGizmo2Adaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaReactiveIdentityProviderImpl";
        Gizmo.create(gizmoAdaptor).class_(name, cc -> {
            cc.extends_(JpaReactiveIdentityProvider.class);
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();
            FieldDesc passwordProviderField = cc.field("passwordProvider", ifc -> {
                ifc.setType(PasswordProvider.class);
                // package-private (default access)
            });

            cc.method("authenticate", mc -> {
                mc.public_();
                mc.returning(Uni.class);
                var sessionParam = mc.parameter("session", Mutiny.Session.class);
                var requestParam = mc.parameter("request", UsernamePasswordAuthenticationRequest.class);
                mc.body(bc -> {
                    LocalVar username = bc.localVar("username", bc.invokeVirtual(
                            MethodDesc.of(UsernamePasswordAuthenticationRequest.class, "getUsername", String.class),
                            requestParam));

                    Expr userUni = lookupUserById(jpaSecurityDefinition, bc, sessionParam, username);

                    // .map(user -> { /* build identity */ })
                    Expr identityUni = bc.invokeInterface(
                            MethodDesc.of(Uni.class, "map", Uni.class, Function.class),
                            userUni, bc.lambda(Function.class, lc -> {
                                Var thisCapture = lc.capture("this", cc.this_());
                                Var reqCapture = lc.capture("request", requestParam);
                                Expr user = lc.parameter("user", 0);
                                lc.body(lbc -> {
                                    buildIdentity(index, jpaSecurityDefinition, passwordTypeValue, passwordProviderValue,
                                            panacheEntityPredicate, passwordProviderField, thisCapture, reqCapture,
                                            user, lbc);
                                });
                            }));

                    bc.return_(identityUni);
                });
            });
        });
    }

    private static void generateTrustedIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, PanacheEntityPredicateBuildItem panacheEntityPredicate) {
        GeneratedBeanGizmo2Adaptor gizmoAdaptor = new GeneratedBeanGizmo2Adaptor(beanProducer);

        String name = jpaSecurityDefinition.annotatedClass.name() + "__JpaReactiveTrustedIdentityProviderImpl";
        Gizmo.create(gizmoAdaptor).class_(name, cc -> {
            cc.extends_(JpaReactiveTrustedIdentityProvider.class);
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();

            cc.method("authenticate", mc -> {
                mc.public_();
                mc.returning(Uni.class);
                var sessionParam = mc.parameter("session", Mutiny.Session.class);
                var requestParam = mc.parameter("request", TrustedAuthenticationRequest.class);
                mc.body(bc -> {
                    LocalVar username = bc.localVar("username", bc.invokeVirtual(
                            MethodDesc.of(TrustedAuthenticationRequest.class, "getPrincipal", String.class),
                            requestParam));

                    Expr userUni = lookupUserById(jpaSecurityDefinition, bc, sessionParam, username);

                    // .map(user -> { /* build identity */ })
                    Expr identityUni = bc.invokeInterface(
                            MethodDesc.of(Uni.class, "map", Uni.class, Function.class),
                            userUni, bc.lambda(Function.class, lc -> {
                                Var reqCapture = lc.capture("request", requestParam);
                                Expr user = lc.parameter("user", 0);
                                lc.body(lbc -> {
                                    buildTrustedIdentity(index, jpaSecurityDefinition, panacheEntityPredicate,
                                            reqCapture, user, lbc);
                                });
                            }));

                    bc.return_(identityUni);
                });
            });
        });
    }

    private static Expr lookupUserById(JpaSecurityDefinition jpaSecurityDefinition, BlockCreator bc,
            Expr session, Expr username) {

        // two strategies, depending on whether the username is natural id
        AnnotationInstance naturalIdAnnotation = jpaSecurityDefinition.username.annotation(NATURAL_ID);

        // PlainUserEntity.class
        String userEntityClassName = jpaSecurityDefinition.annotatedClass.name().toString();
        Expr userEntityClass = bc.classForName(Const.of(userEntityClassName));

        boolean fetchJoinRoles = shouldFetchJoinRoles(jpaSecurityDefinition);
        Expr user;
        if (naturalIdAnnotation != null) {

            // Identifier.id("name", username)
            Expr id = bc.invokeStatic(
                    MethodDesc.of(Identifier.class, "id", Identifier.Id.class, String.class, Object.class),
                    Const.of(jpaSecurityDefinition.username.name()), username);

            // session.find(PlainUserEntity.class, Identifier.id("name", username))
            user = bc.invokeInterface(
                    MethodDesc.of(Mutiny.Session.class, "find", Uni.class, Class.class, Identifier.class),
                    session, userEntityClass, id);

            if (fetchJoinRoles) {
                // .flatMap(user -> session.fetch(user))
                user = bc.invokeInterface(
                        MethodDesc.of(Uni.class, "flatMap", Uni.class, Function.class),
                        user, bc.lambda(Function.class, lc -> {
                            Var sessionCapture = lc.capture("session", session);
                            Expr user1 = lc.parameter("user1", 0);
                            lc.body(lbc -> {
                                lbc.return_(lbc.invokeInterface(
                                        MethodDesc.of(Mutiny.Session.class, "fetch", Uni.class, Object.class),
                                        sessionCapture, user1));
                            });
                        }));
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
            Expr query1 = bc.invokeInterface(
                    MethodDesc.of(Mutiny.Session.class, "createQuery", Mutiny.SelectionQuery.class, String.class, Class.class),
                    session, Const.of(hql), userEntityClass);

            // .setParameter("name", username)
            Expr query2 = bc.invokeInterface(
                    MethodDesc.of(Mutiny.SelectionQuery.class, "setParameter", Mutiny.SelectionQuery.class, String.class,
                            Object.class),
                    query1, Const.of("name"), username);

            // .getSingleResultOrNull()
            user = bc.invokeInterface(
                    MethodDesc.of(Mutiny.SelectionQuery.class, "getSingleResultOrNull", Uni.class),
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
}
