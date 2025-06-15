package io.quarkus.security.jpa.common.deployment;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.PasswordType;
import io.quarkus.security.jpa.RolesValue;
import io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public final class JpaSecurityIdentityUtil {

    private static final DotName DOTNAME_SET = DotName.createSimple(Set.class.getName());
    private static final DotName DOTNAME_COLLECTION = DotName.createSimple(Collection.class.getName());
    private static final DotName DOTNAME_ROLES_VALUE = DotName.createSimple(RolesValue.class.getName());

    private JpaSecurityIdentityUtil() {
        // util class
    }

    public static void buildIdentity(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            AnnotationValue passwordTypeValue, AnnotationValue passwordProviderValue,
            PanacheEntityPredicateBuildItem panacheEntityPredicate, FieldDescriptor passwordProviderField,
            MethodCreator outerMethod, ResultHandle userVar, BytecodeCreator innerMethod) {
        // if(user == null) throw new AuthenticationFailedException();

        PasswordType passwordType = passwordTypeValue != null ? PasswordType.valueOf(passwordTypeValue.asEnum())
                : PasswordType.MCF;

        try (BytecodeCreator trueBranch = innerMethod.ifNull(userVar).trueBranch()) {

            ResultHandle exceptionInstance = trueBranch
                    .newInstance(MethodDescriptor.ofConstructor(AuthenticationFailedException.class));
            trueBranch.invokeStaticMethod(passwordActionMethod(), trueBranch.load(passwordType));
            trueBranch.throwException(exceptionInstance);
        }

        // :pass = user.pass | user.getPass()
        ResultHandle pass = jpaSecurityDefinition.password.readValue(innerMethod, userVar);

        if (passwordType == PasswordType.CUSTOM && passwordProviderValue == null) {
            throw new RuntimeException("Missing password provider for password type: " + passwordType);
        }

        ResultHandle storedPassword;
        switch (passwordType) {
            case CUSTOM:
                String passwordProviderClassStr = passwordProviderValue.asString();
                String passwordProviderMethod = "getPassword";
                ResultHandle passwordProviderInstanceField = innerMethod.readInstanceField(passwordProviderField,
                        outerMethod.getThis());
                BytecodeCreator trueBranch = innerMethod.ifNull(passwordProviderInstanceField).trueBranch();
                ResultHandle passwordProviderInstance = trueBranch
                        .newInstance(MethodDescriptor.ofConstructor(passwordProviderClassStr));
                trueBranch.writeInstanceField(passwordProviderField, outerMethod.getThis(), passwordProviderInstance);
                trueBranch.close();
                ResultHandle objectToInvokeOn = innerMethod.readInstanceField(passwordProviderField,
                        outerMethod.getThis());

                // :getPasswordMethod(:pass);
                storedPassword = innerMethod
                        .invokeVirtualMethod(
                                MethodDescriptor.ofMethod(passwordProviderClassStr, passwordProviderMethod,
                                        org.wildfly.security.password.Password.class, String.class),
                                objectToInvokeOn, pass);
                break;
            case CLEAR:
                storedPassword = innerMethod.invokeStaticMethod(getUtilMethod("getClearPassword"), pass);
                break;
            case MCF:
                storedPassword = innerMethod.invokeStaticMethod(getUtilMethod("getMcfPassword"), pass);
                break;
            default:
                throw new RuntimeException("Unknown password type: " + passwordType);
        }

        // Builder builder = JpaIdentityProviderUtil.checkPassword(storedPassword, request);
        ResultHandle builder = innerMethod.invokeStaticMethod(MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class,
                "checkPassword", QuarkusSecurityIdentity.Builder.class, org.wildfly.security.password.Password.class,
                UsernamePasswordAuthenticationRequest.class), storedPassword, outerMethod.getMethodParam(1));
        AssignableResultHandle builderVar = innerMethod.createVariable(QuarkusSecurityIdentity.Builder.class);
        innerMethod.assign(builderVar, builder);

        setupRoles(index, jpaSecurityDefinition, panacheEntityPredicate, userVar, builderVar, innerMethod);
    }

    public static void buildTrustedIdentity(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            PanacheEntityPredicateBuildItem panacheEntityPredicate, MethodCreator outerMethod, ResultHandle userVar,
            BytecodeCreator innerMethod) {
        // if(user == null) return null;
        try (BytecodeCreator trueBranch = innerMethod.ifNull(userVar).trueBranch()) {
            trueBranch.returnValue(trueBranch.loadNull());
        }
        // Builder builder = JpaIdentityProviderUtil.trusted(request);
        ResultHandle builder = innerMethod.invokeStaticMethod(
                MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class, "trusted",
                        QuarkusSecurityIdentity.Builder.class, TrustedAuthenticationRequest.class),
                outerMethod.getMethodParam(1));
        AssignableResultHandle builderVar = innerMethod.createVariable(QuarkusSecurityIdentity.Builder.class);
        innerMethod.assign(builderVar, builder);

        setupRoles(index, jpaSecurityDefinition, panacheEntityPredicate, userVar, builderVar, innerMethod);
    }

    static AnnotationTarget getSingleAnnotatedElement(Index index, DotName annotation) {
        List<AnnotationInstance> annotations = index.getAnnotations(annotation);
        if (annotations.isEmpty()) {
            return null;
        } else if (annotations.size() > 1) {
            throw new RuntimeException("You can only annotate one field or method with @" + annotation);
        }
        return annotations.get(0).target();
    }

    private static void setupRoles(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            PanacheEntityPredicateBuildItem panacheEntityPredicate, ResultHandle userVar,
            AssignableResultHandle builderVar, BytecodeCreator innerMethod) {
        ResultHandle role = jpaSecurityDefinition.roles.readValue(innerMethod, userVar);
        // role: user.getRole()
        boolean handledRole = false;
        Type rolesType = jpaSecurityDefinition.roles.type();
        switch (rolesType.kind()) {
            case ARRAY:
                // FIXME: support non-JPA-backed array roles?
                break;
            case CLASS:
                if (rolesType.name().equals(DotNames.STRING)) {
                    // JpaIdentityProviderUtil.addRoles(builder, :role)
                    innerMethod.invokeStaticMethod(MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class, "addRoles",
                            void.class, QuarkusSecurityIdentity.Builder.class, String.class), builderVar, role);
                    handledRole = true;
                }
                break;
            case PARAMETERIZED_TYPE:
                DotName roleType = rolesType.name();
                if (roleType.equals(DotNames.LIST) || roleType.equals(DOTNAME_COLLECTION)
                        || roleType.equals(DOTNAME_SET)) {
                    Type elementType = rolesType.asParameterizedType().arguments().get(0);
                    String elementClassName = elementType.name().toString();
                    String elementClassTypeDescriptor = "L" + elementClassName.replace('.', '/') + ";";
                    JpaSecurityDefinition.FieldOrMethod rolesFieldOrMethod;
                    if (!elementType.name().equals(DotNames.STRING)) {
                        ClassInfo roleClass = index.getClassByName(elementType.name());
                        if (roleClass == null) {
                            throw new RuntimeException(
                                    "The role element type must be indexed by Jandex: " + elementType);
                        }
                        AnnotationTarget annotatedRolesValue = getSingleAnnotatedElement(index, DOTNAME_ROLES_VALUE);
                        rolesFieldOrMethod = JpaSecurityDefinition.getFieldOrMethod(index, roleClass,
                                annotatedRolesValue, panacheEntityPredicate.isPanache(roleClass));
                        if (rolesFieldOrMethod == null) {
                            throw new RuntimeException(
                                    "Missing @RoleValue annotation on (non-String) role element type: " + elementType);
                        }
                    } else {
                        rolesFieldOrMethod = null;
                    }
                    // for(:elementType roleElement : :role){
                    // JpaIdentityProviderUtil.addRoles(:role.roleField);
                    // // or for String collections:
                    // JpaIdentityProviderUtil.addRoles(:role);
                    // }
                    foreach(innerMethod, role, elementClassTypeDescriptor, (creator, var) -> {
                        ResultHandle roleElement;
                        if (rolesFieldOrMethod != null) {
                            roleElement = rolesFieldOrMethod.readValue(creator, var);
                        } else {
                            roleElement = var;
                        }
                        creator.invokeStaticMethod(MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class, "addRoles",
                                void.class, QuarkusSecurityIdentity.Builder.class, String.class), builderVar,
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
        innerMethod.returnValue(innerMethod.invokeVirtualMethod(MethodDescriptor
                .ofMethod(QuarkusSecurityIdentity.Builder.class, "build", QuarkusSecurityIdentity.class), builderVar));
    }

    private static void foreach(BytecodeCreator bytecodeCreator, ResultHandle iterable, String type,
            BiConsumer<BytecodeCreator, AssignableResultHandle> body) {
        ResultHandle iterator = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), iterable);
        try (BytecodeCreator loop = bytecodeCreator.createScope()) {
            ResultHandle hasNextValue = loop.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class), iterator);

            BranchResult hasNextBranch = loop.ifNonZero(hasNextValue);
            try (BytecodeCreator hasNext = hasNextBranch.trueBranch()) {
                ResultHandle next = hasNext.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Iterator.class, "next", Object.class), iterator);

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

    private static MethodDescriptor getUtilMethod(String passwordProviderMethod) {
        return MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class, passwordProviderMethod,
                org.wildfly.security.password.Password.class, String.class);
    }

    private static MethodDescriptor passwordActionMethod() {
        return MethodDescriptor.ofMethod(JpaIdentityProviderUtil.class, "passwordAction", void.class,
                PasswordType.class);
    }
}
