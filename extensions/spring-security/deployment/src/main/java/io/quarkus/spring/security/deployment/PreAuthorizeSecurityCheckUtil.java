package io.quarkus.spring.security.deployment;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.check.SecurityCheck;
import io.quarkus.spring.security.deployment.roles.HasRoleValueProducer;
import io.quarkus.spring.security.runtime.interceptor.check.AllDelegatingSecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.AnonymousCheck;
import io.quarkus.spring.security.runtime.interceptor.check.AnyDelegatingSecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.PrincipalNameFromParameterObjectSecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.PrincipalNameFromParameterSecurityCheck;

/**
 * Utility used to provide access to instances of the appropriate SecurityCheck classes
 */
final class PreAuthorizeSecurityCheckUtil {

    private PreAuthorizeSecurityCheckUtil() {
    }

    public static Function<BytecodeCreator, ResultHandle> anonymousSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.readStaticField(of(AnonymousCheck.class, "INSTANCE", AnonymousCheck.class));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> hasRoleSecurityCheck(
            List<HasRoleValueProducer> hasRoleValueProducers) {
        return new HasRoleSecurityCheck(hasRoleValueProducers);
    }

    public static Function<BytecodeCreator, ResultHandle> principalNameFromParameterSecurityCheck(int index,
            PrincipalNameFromParameterSecurityCheck.CheckType checkType) {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.invokeStaticMethod(
                        ofMethod(PrincipalNameFromParameterSecurityCheck.class, "of",
                                PrincipalNameFromParameterSecurityCheck.class, int.class,
                                PrincipalNameFromParameterSecurityCheck.CheckType.class),
                        creator.load(index),
                        creator.readStaticField(FieldDescriptor.of(PrincipalNameFromParameterSecurityCheck.CheckType.class,
                                checkType.toString(), PrincipalNameFromParameterSecurityCheck.CheckType.class)));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> principalNameFromParameterObjectSecurityCheck(int index,
            String expectedParameterClass, String stringPropertyAccessorClass, String propertyName) {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.invokeStaticMethod(
                        ofMethod(PrincipalNameFromParameterObjectSecurityCheck.class, "of",
                                PrincipalNameFromParameterObjectSecurityCheck.class, int.class, String.class, String.class,
                                String.class),
                        creator.load(index), creator.load(expectedParameterClass),
                        creator.load(stringPropertyAccessorClass), creator.load(propertyName));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> generateAllDelegatingSecurityCheck(
            List<Function<BytecodeCreator, ResultHandle>> delegatesList) {
        return generateDelegatingSecurityCheck(delegatesList, AllDelegatingSecurityCheck.class);
    }

    public static Function<BytecodeCreator, ResultHandle> generateAnyDelegatingSecurityCheck(
            List<Function<BytecodeCreator, ResultHandle>> delegatesList) {
        return generateDelegatingSecurityCheck(delegatesList, AnyDelegatingSecurityCheck.class);
    }

    private static Function<BytecodeCreator, ResultHandle> generateDelegatingSecurityCheck(
            List<Function<BytecodeCreator, ResultHandle>> delegatesList, Class<? extends SecurityCheck> securityCheckClass) {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                ResultHandle delegates = creator.newInstance(ofConstructor(ArrayList.class, int.class),
                        creator.load(delegatesList.size()));
                for (Function<BytecodeCreator, ResultHandle> delegateFunction : delegatesList) {
                    ResultHandle delegate = delegateFunction.apply(creator);
                    creator.invokeVirtualMethod(ofMethod(ArrayList.class, "add", boolean.class, Object.class), delegates,
                            delegate);
                }
                return creator.newInstance(ofConstructor(securityCheckClass, List.class), delegates);
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> beanMethodGeneratedSecurityCheck(String generatedClassName) {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.invokeStaticMethod(ofMethod(generatedClassName, "getInstance", generatedClassName));
            }
        };
    }

    private static class HasRoleSecurityCheck implements Function<BytecodeCreator, ResultHandle> {
        private final List<HasRoleValueProducer> roleValueProducers;

        private HasRoleSecurityCheck(List<HasRoleValueProducer> roleValueProducers) {
            this.roleValueProducers = roleValueProducers;
        }

        @Override
        public ResultHandle apply(BytecodeCreator creator) {
            ResultHandle rolesAllowedArgs = creator.newArray(String.class, roleValueProducers.size());
            int i = 0;
            for (Function<BytecodeCreator, ResultHandle> roleValueProducer : roleValueProducers) {
                creator.writeArrayValue(rolesAllowedArgs, i++, roleValueProducer.apply(creator));
            }

            return creator.invokeStaticMethod(
                    ofMethod(RolesAllowedCheck.class, "of", RolesAllowedCheck.class, String[].class), rolesAllowedArgs);
        }
    }

}
