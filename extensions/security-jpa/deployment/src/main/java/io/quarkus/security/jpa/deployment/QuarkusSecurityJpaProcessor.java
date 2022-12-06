package io.quarkus.security.jpa.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.Session;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.PasswordProvider;
import io.quarkus.security.jpa.PasswordType;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.RolesValue;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import io.quarkus.security.jpa.deployment.JpaSecurityDefinition.FieldOrMethod;
import io.quarkus.security.jpa.runtime.JpaIdentityProvider;
import io.quarkus.security.jpa.runtime.JpaTrustedIdentityProvider;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

class QuarkusSecurityJpaProcessor {

    static final DotName DOTNAME_OBJECT = DotName.createSimple(Object.class.getName());

    private static final DotName DOTNAME_STRING = DotName.createSimple(String.class.getName());
    private static final DotName DOTNAME_LIST = DotName.createSimple(List.class.getName());
    private static final DotName DOTNAME_SET = DotName.createSimple(Set.class.getName());
    private static final DotName DOTNAME_COLLECTION = DotName.createSimple(Collection.class.getName());

    private static final DotName DOTNAME_NATURAL_ID = DotName.createSimple(NaturalId.class.getName());

    private static final DotName DOTNAME_USER_DEFINITION = DotName.createSimple(UserDefinition.class.getName());
    private static final DotName DOTNAME_USERNAME = DotName.createSimple(Username.class.getName());
    private static final DotName DOTNAME_PASSWORD = DotName.createSimple(Password.class.getName());
    private static final DotName DOTNAME_ROLES = DotName.createSimple(Roles.class.getName());
    private static final DotName DOTNAME_ROLES_VALUE = DotName.createSimple(RolesValue.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JPA);
    }

    @BuildStep
    void configureJpaAuthConfig(ApplicationIndexBuildItem index,
            BuildProducer<UnremovableBeanBuildItem> unremovable,
            BuildProducer<GeneratedBeanBuildItem> beanProducer,
            List<PanacheEntityClassesBuildItem> panacheEntityClasses) throws Exception {

        // Generate an IdentityProvider if we have a @UserDefinition
        List<AnnotationInstance> userDefinitions = index.getIndex().getAnnotations(DOTNAME_USER_DEFINITION);
        if (userDefinitions.size() > 1) {
            throw new RuntimeException("You can only annotate one class with @UserDefinition");
        } else if (!userDefinitions.isEmpty()) {
            ClassInfo userDefinitionClass = userDefinitions.get(0).target().asClass();
            AnnotationTarget annotatedUsername = getSingleAnnotatedElement(index.getIndex(), DOTNAME_USERNAME);
            // FIXME: check that it's in the same class hierarchy
            // FIXME: look up fields with default names? name, username…
            AnnotationTarget annotatedPassword = getSingleAnnotatedElement(index.getIndex(), DOTNAME_PASSWORD);
            AnnotationTarget annotatedRoles = getSingleAnnotatedElement(index.getIndex(), DOTNAME_ROLES);
            Set<String> panacheEntities = collectPanacheEntities(panacheEntityClasses);
            // collect associated getters if required
            JpaSecurityDefinition jpaSecurityDefinition = new JpaSecurityDefinition(index.getIndex(),
                    userDefinitionClass,
                    isPanache(userDefinitionClass, panacheEntities),
                    annotatedUsername,
                    annotatedPassword,
                    annotatedRoles);
            AnnotationInstance passAnnotation = jpaSecurityDefinition.password.annotation(DOTNAME_PASSWORD);
            AnnotationValue passwordType = passAnnotation.value();
            AnnotationValue customPasswordProvider = passAnnotation.value("provider");

            generateIdentityProvider(index.getIndex(), jpaSecurityDefinition,
                    passwordType, customPasswordProvider, beanProducer, panacheEntities);

            generateTrustedIdentityProvider(index.getIndex(), jpaSecurityDefinition,
                    beanProducer, panacheEntities);
        }
    }

    private boolean isPanache(ClassInfo annotatedClass, Set<String> panacheEntities) {
        return panacheEntities.contains(annotatedClass.name().toString());
    }

    private Set<String> collectPanacheEntities(List<PanacheEntityClassesBuildItem> panacheEntityClassesBuildItems) {
        Set<String> modelClasses = new HashSet<>();
        for (PanacheEntityClassesBuildItem panacheEntityClasses : panacheEntityClassesBuildItems) {
            modelClasses.addAll(panacheEntityClasses.getEntityClasses());
        }
        return modelClasses;
    }

    private AnnotationTarget getSingleAnnotatedElement(Index index, DotName annotation) {
        List<AnnotationInstance> annotations = index.getAnnotations(annotation);
        if (annotations.isEmpty()) {
            return null;
        } else if (annotations.size() > 1) {
            throw new RuntimeException("You can only annotate one field or method with @" + annotation);
        }
        return annotations.get(0).target();
    }

    private void generateIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            AnnotationValue passwordTypeValue, AnnotationValue passwordProviderValue,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, Set<String> panacheClasses) {
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

                // if(user == null) throw new AuthenticationFailedException();
                try (BytecodeCreator trueBranch = methodCreator.ifNull(userVar).trueBranch()) {
                    ResultHandle exceptionInstance = trueBranch
                            .newInstance(MethodDescriptor.ofConstructor(AuthenticationFailedException.class));
                    trueBranch.throwException(exceptionInstance);
                }

                // :pass = user.pass | user.getPass()
                ResultHandle pass = jpaSecurityDefinition.password.readValue(methodCreator, userVar);

                PasswordType passwordType = passwordTypeValue != null ? PasswordType.valueOf(passwordTypeValue.asEnum())
                        : PasswordType.MCF;

                if (passwordType == PasswordType.CUSTOM && passwordProviderValue == null) {
                    throw new RuntimeException("Missing password provider for password type: " + passwordType);
                }

                ResultHandle objectToInvokeOn;
                String passwordProviderClassStr;
                String passwordProviderMethod;
                switch (passwordType) {
                    case CUSTOM:
                        passwordProviderClassStr = passwordProviderValue.asString();
                        passwordProviderMethod = "getPassword";
                        ResultHandle passwordProviderInstanceField = methodCreator.readInstanceField(passwordProviderField,
                                methodCreator.getThis());
                        BytecodeCreator trueBranch = methodCreator.ifNull(passwordProviderInstanceField).trueBranch();
                        ResultHandle passwordProviderInstance = trueBranch
                                .newInstance(MethodDescriptor.ofConstructor(passwordProviderClassStr));
                        trueBranch.writeInstanceField(passwordProviderField, trueBranch.getThis(), passwordProviderInstance);
                        trueBranch.close();
                        objectToInvokeOn = methodCreator.readInstanceField(passwordProviderField, methodCreator.getThis());
                        break;
                    case CLEAR:
                        passwordProviderClassStr = name;
                        passwordProviderMethod = "getClearPassword";
                        objectToInvokeOn = methodCreator.getThis();
                        break;
                    case MCF:
                        passwordProviderClassStr = name;
                        passwordProviderMethod = "getMcfPassword";
                        objectToInvokeOn = methodCreator.getThis();
                        break;
                    default:
                        throw new RuntimeException("Unknown password type: " + passwordType);
                }

                // :getPasswordMethod(:pass);
                ResultHandle storedPassword = methodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(passwordProviderClassStr, passwordProviderMethod,
                                org.wildfly.security.password.Password.class,
                                String.class),
                        objectToInvokeOn, pass);

                // Builder builder = checkPassword(storedPassword, request);
                ResultHandle builder = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(name, "checkPassword",
                        QuarkusSecurityIdentity.Builder.class,
                        org.wildfly.security.password.Password.class,
                        UsernamePasswordAuthenticationRequest.class),
                        methodCreator.getThis(),
                        storedPassword, methodCreator.getMethodParam(1));
                AssignableResultHandle builderVar = methodCreator.createVariable(QuarkusSecurityIdentity.Builder.class);
                methodCreator.assign(builderVar, builder);

                setupRoles(index, jpaSecurityDefinition, panacheClasses, name, methodCreator, userVar, builderVar);
            }
        }
    }

    private void generateTrustedIdentityProvider(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            BuildProducer<GeneratedBeanBuildItem> beanProducer, Set<String> panacheClasses) {
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

                // if(user == null) return null;
                try (BytecodeCreator trueBranch = methodCreator.ifNull(userVar).trueBranch()) {
                    trueBranch.returnValue(trueBranch.loadNull());
                }
                // Builder builder = trusted(request);
                ResultHandle builder = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(name, "trusted",
                        QuarkusSecurityIdentity.Builder.class,
                        TrustedAuthenticationRequest.class),
                        methodCreator.getThis(),
                        methodCreator.getMethodParam(1));
                AssignableResultHandle builderVar = methodCreator.createVariable(QuarkusSecurityIdentity.Builder.class);
                methodCreator.assign(builderVar, builder);

                setupRoles(index, jpaSecurityDefinition, panacheClasses, name, methodCreator, userVar, builderVar);
            }
        }
    }

    private void setupRoles(Index index, JpaSecurityDefinition jpaSecurityDefinition, Set<String> panacheClasses, String name,
            MethodCreator methodCreator, AssignableResultHandle userVar, AssignableResultHandle builderVar) {
        ResultHandle role = jpaSecurityDefinition.roles.readValue(methodCreator, userVar);
        // role: user.getRole()
        boolean handledRole = false;
        Type rolesType = jpaSecurityDefinition.roles.type();
        switch (rolesType.kind()) {
            case ARRAY:
                // FIXME: support non-JPA-backed array roles?
                break;
            case CLASS:
                if (rolesType.name().equals(DOTNAME_STRING)) {
                    // addRoles(builder, :role)
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(name, "addRoles", void.class,
                                    QuarkusSecurityIdentity.Builder.class, String.class),
                            methodCreator.getThis(),
                            builderVar,
                            role);
                    handledRole = true;
                }
                break;
            case PARAMETERIZED_TYPE:
                DotName roleType = rolesType.name();
                if (roleType.equals(DOTNAME_LIST)
                        || roleType.equals(DOTNAME_COLLECTION)
                        || roleType.equals(DOTNAME_SET)) {
                    Type elementType = rolesType.asParameterizedType().arguments().get(0);
                    String elementClassName = elementType.name().toString();
                    String elementClassTypeDescriptor = "L" + elementClassName.replace('.', '/') + ";";
                    FieldOrMethod rolesFieldOrMethod;
                    if (!elementType.name().equals(DOTNAME_STRING)) {
                        ClassInfo roleClass = index.getClassByName(elementType.name());
                        if (roleClass == null) {
                            throw new RuntimeException(
                                    "The role element type must be indexed by Jandex: " + elementType);
                        }
                        AnnotationTarget annotatedRolesValue = getSingleAnnotatedElement(index, DOTNAME_ROLES_VALUE);
                        rolesFieldOrMethod = JpaSecurityDefinition.getFieldOrMethod(index, roleClass,
                                annotatedRolesValue, isPanache(roleClass, panacheClasses));
                        if (rolesFieldOrMethod == null) {
                            throw new RuntimeException(
                                    "Missing @RoleValue annotation on (non-String) role element type: " + elementType);
                        }
                    } else {
                        rolesFieldOrMethod = null;
                    }
                    // for(:elementType roleElement : :role){
                    //    ret.addRoles(:role.roleField);
                    //    // or for String collections:
                    //    ret.addRoles(:role);
                    // }
                    foreach(methodCreator, role, elementClassTypeDescriptor, (creator, var) -> {
                        ResultHandle roleElement;
                        if (rolesFieldOrMethod != null) {
                            roleElement = rolesFieldOrMethod.readValue(creator, var);
                        } else {
                            roleElement = var;
                        }
                        creator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(name, "addRoles", void.class,
                                        QuarkusSecurityIdentity.Builder.class, String.class),
                                methodCreator.getThis(),
                                builderVar,
                                roleElement);
                    });
                    handledRole = true;
                }
                break;
        }
        if (!handledRole) {
            throw new RuntimeException("Unsupported @Roles field/getter type: " + rolesType);
        }

        // return builder.build()
        methodCreator.returnValue(methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(QuarkusSecurityIdentity.Builder.class,
                        "build",
                        QuarkusSecurityIdentity.class),
                builderVar));
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

    private void foreach(MethodCreator method, ResultHandle iterable, String type,
            BiConsumer<BytecodeCreator, AssignableResultHandle> body) {
        ResultHandle iterator = method.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class),
                iterable);
        try (BytecodeCreator loop = method.createScope()) {
            ResultHandle hasNextValue = loop.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                    iterator);

            BranchResult hasNextBranch = loop.ifNonZero(hasNextValue);
            try (BytecodeCreator hasNext = hasNextBranch.trueBranch()) {
                ResultHandle next = hasNext.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                        iterator);

                AssignableResultHandle var = hasNext.createVariable(type);
                hasNext.assign(var, next);

                body.accept(hasNext, var);
                hasNext.continueScope(loop);
            }
            try (BytecodeCreator doesNotHaveNext = hasNextBranch.falseBranch()) {
                doesNotHaveNext.breakScope(loop);
            }
        }
    }
}
