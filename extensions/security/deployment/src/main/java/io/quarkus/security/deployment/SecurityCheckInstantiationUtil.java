package io.quarkus.security.deployment;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.function.Function;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;

public class SecurityCheckInstantiationUtil {

    private SecurityCheckInstantiationUtil() {
    }

    public static Function<BytecodeCreator, ResultHandle> rolesAllowedSecurityCheck(String... rolesAllowed) {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                if ((rolesAllowed == null)) {
                    throw new IllegalStateException(
                            "Cannot use a null array to create an instance of " + RolesAllowedCheck.class.getName());
                }

                ResultHandle rolesAllowedArgs = creator.newArray(String.class, rolesAllowed.length);
                int i = 0;
                for (String val : rolesAllowed) {
                    creator.writeArrayValue(rolesAllowedArgs, i++, creator.load(val));
                }
                return creator.invokeStaticMethod(
                        ofMethod(RolesAllowedCheck.class, "of", RolesAllowedCheck.class, String[].class), rolesAllowedArgs);
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> denyAllSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.readStaticField(of(DenyAllCheck.class, "INSTANCE", DenyAllCheck.class));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> permitAllSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.readStaticField(of(PermitAllCheck.class, "INSTANCE", PermitAllCheck.class));
            }
        };
    }

    public static Function<BytecodeCreator, ResultHandle> authenticatedSecurityCheck() {
        return new Function<BytecodeCreator, ResultHandle>() {
            @Override
            public ResultHandle apply(BytecodeCreator creator) {
                return creator.readStaticField(of(AuthenticatedCheck.class, "INSTANCE", AuthenticatedCheck.class));
            }
        };
    }
}
