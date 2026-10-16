package io.quarkus.security.jpa.common.deployment;

import java.lang.constant.ClassDesc;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.wildfly.security.password.Password;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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
            PanacheEntityPredicateBuildItem panacheEntityPredicate, FieldDesc passwordProviderField,
            Expr thisRef, Expr requestParam, Expr userVar, BlockCreator bc) {
        // if(user == null) throw new AuthenticationFailedException();

        PasswordType passwordType = passwordTypeValue != null ? PasswordType.valueOf(passwordTypeValue.asEnum())
                : PasswordType.MCF;

        bc.if_(bc.isNull(userVar), trueBranch -> {
            Expr exceptionInstance = trueBranch
                    .new_(ConstructorDesc.of(AuthenticationFailedException.class));
            trueBranch.invokeStatic(passwordActionMethod(), Const.of(passwordType));
            trueBranch.throw_(exceptionInstance);
        });

        // :pass = user.pass | user.getPass()
        LocalVar pass = bc.localVar("pass", jpaSecurityDefinition.password.readValue(bc, userVar));

        if (passwordType == PasswordType.CUSTOM && passwordProviderValue == null) {
            throw new RuntimeException("Missing password provider for password type: " + passwordType);
        }

        Expr storedPassword;
        switch (passwordType) {
            case CUSTOM:
                String passwordProviderClassStr = passwordProviderValue.asString();
                String passwordProviderMethod = "getPassword";
                LocalVar passwordProviderInstanceField = bc.localVar("ppField",
                        bc.get(thisRef.field(passwordProviderField)));
                bc.if_(bc.isNull(passwordProviderInstanceField), trueBranch -> {
                    Expr passwordProviderInstance = trueBranch
                            .new_(ConstructorDesc.of(ClassDesc.of(passwordProviderClassStr)));
                    trueBranch.set(thisRef.field(passwordProviderField), passwordProviderInstance);
                });
                LocalVar objectToInvokeOn = bc.localVar("ppObj",
                        bc.get(thisRef.field(passwordProviderField)));

                // :getPasswordMethod(:pass);
                storedPassword = bc.invokeVirtual(
                        ClassMethodDesc.of(ClassDesc.of(passwordProviderClassStr),
                                passwordProviderMethod,
                                Password.class,
                                String.class),
                        bc.cast(objectToInvokeOn, ClassDesc.of(passwordProviderClassStr)), pass);
                break;
            case CLEAR:
                storedPassword = bc.invokeStatic(getUtilMethod("getClearPassword"), pass);
                break;
            case MCF:
                storedPassword = bc.invokeStatic(getUtilMethod("getMcfPassword"), pass);
                break;
            default:
                throw new RuntimeException("Unknown password type: " + passwordType);
        }

        // Builder builder = JpaIdentityProviderUtil.checkPassword(storedPassword, request);
        Expr builder = bc.invokeStatic(
                MethodDesc.of(JpaIdentityProviderUtil.class, "checkPassword",
                        QuarkusSecurityIdentity.Builder.class,
                        Password.class,
                        UsernamePasswordAuthenticationRequest.class),
                storedPassword, requestParam);
        LocalVar builderVar = bc.localVar("builder", QuarkusSecurityIdentity.Builder.class, builder);

        setupRoles(index, jpaSecurityDefinition, panacheEntityPredicate, userVar, builderVar, bc);
    }

    public static void buildTrustedIdentity(Index index, JpaSecurityDefinition jpaSecurityDefinition,
            PanacheEntityPredicateBuildItem panacheEntityPredicate, Expr requestParam, Expr userVar,
            BlockCreator bc) {
        // if(user == null) return null;
        bc.if_(bc.isNull(userVar), trueBranch -> {
            trueBranch.returnNull();
        });
        // Builder builder = JpaIdentityProviderUtil.trusted(request);
        Expr builder = bc.invokeStatic(MethodDesc.of(JpaIdentityProviderUtil.class,
                "trusted",
                QuarkusSecurityIdentity.Builder.class,
                TrustedAuthenticationRequest.class),
                requestParam);
        LocalVar builderVar = bc.localVar("builder", QuarkusSecurityIdentity.Builder.class, builder);

        setupRoles(index, jpaSecurityDefinition, panacheEntityPredicate, userVar, builderVar, bc);
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
            PanacheEntityPredicateBuildItem panacheEntityPredicate, Expr userVar,
            LocalVar builderVar, BlockCreator bc) {
        Expr role = jpaSecurityDefinition.roles.readValue(bc, userVar);
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
                    bc.invokeStatic(
                            MethodDesc.of(JpaIdentityProviderUtil.class, "addRoles", void.class,
                                    QuarkusSecurityIdentity.Builder.class, String.class),
                            builderVar, role);
                    handledRole = true;
                }
                break;
            case PARAMETERIZED_TYPE:
                DotName roleType = rolesType.name();
                if (roleType.equals(DotNames.LIST)
                        || roleType.equals(DOTNAME_COLLECTION)
                        || roleType.equals(DOTNAME_SET)) {
                    Type elementType = rolesType.asParameterizedType().arguments().get(0);
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
                    //    JpaIdentityProviderUtil.addRoles(:role.roleField);
                    //    // or for String collections:
                    //    JpaIdentityProviderUtil.addRoles(:role);
                    // }
                    bc.forEach(role, (loopBody, var) -> {
                        Expr roleElement;
                        if (rolesFieldOrMethod != null) {
                            roleElement = rolesFieldOrMethod.readValue(loopBody, var);
                        } else {
                            roleElement = var;
                        }
                        loopBody.invokeStatic(
                                MethodDesc.of(JpaIdentityProviderUtil.class, "addRoles", void.class,
                                        QuarkusSecurityIdentity.Builder.class, String.class),
                                builderVar, roleElement);
                    });
                    handledRole = true;
                }
                break;
        }
        if (!handledRole) {
            throw new RuntimeException("Unsupported @Roles field/getter type: " + rolesType);
        }

        // return builder.build()
        bc.return_(bc.invokeVirtual(
                MethodDesc.of(QuarkusSecurityIdentity.Builder.class,
                        "build",
                        QuarkusSecurityIdentity.class),
                builderVar));
    }

    private static MethodDesc getUtilMethod(String passwordProviderMethod) {
        return MethodDesc.of(JpaIdentityProviderUtil.class, passwordProviderMethod,
                Password.class, String.class);
    }

    private static MethodDesc passwordActionMethod() {
        return MethodDesc.of(JpaIdentityProviderUtil.class, "passwordAction", void.class, PasswordType.class);
    }
}
